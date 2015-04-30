package com.gameminers.visage.slave;

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
	protected Connection conn;
	protected Channel channel;
	protected List<RenderThread> threads = Lists.newArrayList();
	protected int idx = 0;
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
			Visage.log.info("Connecting to RabbitMQ at "+config.getString("rabbitmq.host")+":"+config.getInt("rabbitmq.port"));
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(config.getString("rabbitmq.host"));
			factory.setPort(config.getInt("rabbitmq.port"));
			if (config.hasPath("rabbitmq.user")) {
				if (Visage.debug) Visage.log.finer("Using authentication");
				factory.setUsername(config.getString("rabbitmq.user"));
				factory.setPassword(config.getString("rabbitmq.password"));
			}
			conn = factory.newConnection();
			Visage.log.info("Setting up "+config.getInt("renderers")+" render threads");
			for (int i = 0; i < config.getInt("renderers"); i++) {
				RenderThread rt = new RenderThread(this);
				threads.add(rt);
				rt.start();
			}
			channel = conn.createChannel();
			String queue = config.getString("rabbitmq.queue");
			if (Visage.debug) Visage.log.finer("Setting up queue '"+queue+"'");
			channel.queueDeclare(queue, false, false, true, null);
			int qos = config.getInt("qos");
			if (qos != -1) {
				channel.basicQos(qos);
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
					} catch (InterruptedException e) {
						break;
					}
				}
			} catch (ShutdownSignalException e) {
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

	@Override
	public void shutdown() {
		run = false;
		interrupt();
	}
}
