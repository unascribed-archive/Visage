/*
 * The MIT License
 *
 * Copyright (c) 2015-2018, Una Thompson (unascribed)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.surgeplay.visage.renderer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.lwjgl.opengl.GL;
import com.github.steveice10.mc.auth.service.SessionService;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.surgeplay.visage.Visage;
import com.surgeplay.visage.VisageRunner;
import com.typesafe.config.Config;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.opengl.GL11.*;

public class VisageRenderer extends Thread implements VisageRunner {
	public static boolean explodeOnError = false;
	public Config config;
	protected SessionService session = new SessionService();
	protected Gson gson = new Gson();
	protected String name;
	protected ConnectionFactory factory;
	protected Connection conn;
	protected Channel channel;
	protected List<RenderContext> threads = Lists.newArrayList();
	protected int idx = 0;
	protected final String queue;
	private boolean run = true;
	public VisageRenderer(Config config) {
		super("Renderer thread");
		this.config = config;
		try {
			name = config.getString("name");
			if (name.startsWith("~")) {
				String command = name.substring(1);
				if (Visage.debug) Visage.log.finer("Running command '"+command+"' to determine the renderer's name");
				Process proc = Runtime.getRuntime().exec(command);
				byte[] bys = ByteStreams.toByteArray(proc.getInputStream());
				name = new String(bys).replace("\n", "").replace("\r", "");
			}
		} catch (Exception e) {
			name = "unnamed renderer";
		}
		Visage.log.info("Renderer name is '"+name+"'");
		queue = config.getString("rabbitmq.queue");
		explodeOnError = config.hasPath("explode-on-error") && config.getBoolean("explode-on-error");
	}
	
	@Override
	public void run() {
		try {
			Visage.log.info("Setting up LWJGL");
			
			if (!glfwInit()) {
				throw new RuntimeException("Failed to initialize GLFW");
			}
			
			glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
			glfwWindowHint(GLFW_DOUBLEBUFFER, GLFW_FALSE);
			
			long testWindow = glfwCreateWindow(1, 1, "Visage GL test", NULL, NULL);
			if (testWindow == NULL) {
				throw new RuntimeException("Failed to create test window");
			}
			glfwMakeContextCurrent(testWindow);
			GL.createCapabilities();
			
			String glV = glGetString(GL_VERSION);
			
			String os = System.getProperty("os.name");
			Visage.log.info("OpenGL "+glV+" on "+os);
			if (os.contains("Win")) {
				Visage.log.severe("Visage does not support Windows. Continue at your own peril!");
			}
			if (!glV.contains("Mesa")) {
				Visage.log.warning("You are using an unsupported graphics driver.");
			}
			if (os.equals("Linux") && glV.contains("Mesa")) {
				Visage.log.fine("Visage fully supports your OS and graphics driver.");
			}
			glfwDestroyWindow(testWindow);
			testWindow = NULL;
			
			
			factory = new ConnectionFactory();
			factory.setHost(config.getString("rabbitmq.host"));
			factory.setPort(config.getInt("rabbitmq.port"));
			factory.setRequestedHeartbeat(10);
			if (config.hasPath("rabbitmq.user")) {
				if (Visage.debug) Visage.log.finer("Using authentication");
				factory.setUsername(config.getString("rabbitmq.user"));
				factory.setPassword(config.getString("rabbitmq.password"));
			}
			reconnect();
			
			Visage.log.info("Setting up "+config.getInt("renderers")+" render threads");
			for (int i = 0; i < config.getInt("renderers"); i++) {
				RenderContext rt = new RenderContext(this);
				threads.add(rt);
				rt.start();
			}
			
			QueueingConsumer consumer = new QueueingConsumer(channel);
			Map<String, Object> args = Maps.newHashMap();
			args.put("x-priority", config.getInt("weight"));
			channel.basicConsume(queue, false, args, consumer);
			Visage.log.info("Listening for jobs");
			try {
				while (run) {
					try {
						Delivery delivery = consumer.nextDelivery();
						if (Visage.debug) Visage.log.finer("Received job, passing on to render thread");
						RenderContext thread = threads.get(idx);
						thread.process(delivery);
						idx++;
						if (idx >= threads.size()) {
							idx = 0;
						}
					} catch (ShutdownSignalException e) {
						try { conn.close(); } catch (Exception ex) {}
						reconnect();
					} catch (InterruptedException e) {
						break;
					}
				}
			} catch (Exception e) {
				Visage.log.log(Level.SEVERE, "A fatal error has occurred in the renderer run loop.", e);
			}
			try {
				Visage.log.info("Shutting down renderer");
				for (RenderContext rt : threads) {
					rt.finish();
				}
				conn.close(5000);
			} catch (Exception e) {
				Visage.log.log(Level.SEVERE, "A fatal error has occurred while shutting down the renderer.", e);
			}
		} catch (Exception e) {
			Visage.log.log(Level.SEVERE, "A fatal error has occurred while setting up the renderer.", e);
		}
	}

	private void reconnect() throws IOException, TimeoutException {
		Visage.log.info("Connecting to RabbitMQ at "+config.getString("rabbitmq.host")+":"+config.getInt("rabbitmq.port"));
		conn = factory.newConnection();
		channel = conn.createChannel();
		if (Visage.debug) Visage.log.finer("Setting up queue '"+queue+"'");
		channel.queueDeclare(queue, false, false, true, null);
		int qos = config.getInt("qos");
		if (qos != -1) {
			channel.basicQos(qos);
		}
	}

	@Override
	public void shutdown() {
		run = false;
		interrupt();
	}
}
