/*
 * Visage
 * Copyright (c) 2015, Aesen Vismea <aesen@gameminers.com>
 *
 * The MIT License
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
package com.gameminers.visage.slave;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.spacehq.mc.auth.SessionService;

import com.gameminers.visage.Visage;
import com.gameminers.visage.VisageRunner;
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
import com.typesafe.config.Config;

public class VisageSlave extends Thread implements VisageRunner {
	protected Config config;
	protected SessionService session = new SessionService();
	protected Gson gson = new Gson();
	protected String name;
	protected ConnectionFactory factory;
	protected Connection conn;
	protected Channel channel;
	protected List<RenderThread> threads = Lists.newArrayList();
	protected int idx = 0;
	protected final String queue;
	private boolean run = true;
	public VisageSlave(Config config) {
		super("Slave thread");
		this.config = config;
		try {
			name = config.getString("name");
			if (name.startsWith("~")) {
				String command = name.substring(1);
				if (Visage.debug) Visage.log.finer("Running command '"+command+"' to determine the slave's name");
				Process proc = Runtime.getRuntime().exec(command);
				byte[] bys = ByteStreams.toByteArray(proc.getInputStream());
				name = new String(bys).replace("\n", "").replace("\r", "");
			}
		} catch (Exception e) {
			name = "unnamed slave";
		}
		Visage.log.info("Slave name is '"+name+"'");
		queue = config.getString("rabbitmq.queue");
	}
	
	@Override
	public void run() {
		System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", Boolean.toString(config.getBoolean("allowSoftware")));
		System.setProperty("org.lwjgl.opengl.Display.noinput", "true");
		try {
			Visage.log.info("Setting up LWJGL");
			Pbuffer test = new Pbuffer(16, 16, new PixelFormat(8, 8, 0), null, null, new ContextAttribs(1, 2));
			test.makeCurrent();
			if (!GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
				Visage.log.severe("Your graphics driver does not support ARB_vertex_buffer_object. The slave cannot continue.");
				test.destroy();
				return;
			}
			String glV = GL11.glGetString(GL11.GL_VERSION);
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
			test.destroy();
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
				RenderThread rt = new RenderThread(this);
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
						RenderThread thread = threads.get(idx);
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
				Visage.log.log(Level.SEVERE, "A fatal error has occurred in the slave run loop.", e);
			}
			try {
				Visage.log.info("Shutting down slave");
				for (RenderThread rt : threads) {
					rt.finish();
				}
				conn.close(5000);
			} catch (Exception e) {
				Visage.log.log(Level.SEVERE, "A fatal error has occurred while shutting down the slave.", e);
			}
		} catch (Exception e) {
			Visage.log.log(Level.SEVERE, "A fatal error has occurred while setting up the slave.", e);
		}
	}

	private void reconnect() throws IOException {
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
