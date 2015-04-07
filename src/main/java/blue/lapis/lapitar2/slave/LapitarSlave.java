package blue.lapis.lapitar2.slave;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.spacehq.mc.auth.SessionService;
import org.spacehq.mc.auth.util.URLUtils;

import blue.lapis.lapitar2.Lapitar;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.typesafe.config.Config;

public class LapitarSlave extends Thread {
	protected Config config;
	protected SessionService session = new SessionService();
	protected Gson gson = new Gson();
	protected BufferedImage steve, alex;
	protected String name;
	protected Connection conn;
	protected Channel channel;
	protected List<RenderThread> threads = Lists.newArrayList();
	protected int idx = 0;
	public LapitarSlave(Config config) {
		super("Slave thread");
		this.config = config;
		try {
			name = config.getString("name");
			if (name.startsWith("~")) {
				String command = name.substring(1);
				Lapitar.log.finer("Running command '"+command+"' to determine the slave's name");
				Process proc = Runtime.getRuntime().exec(command);
				byte[] bys = ByteStreams.toByteArray(proc.getInputStream());
				name = new String(bys).replace("\n", "").replace("\r", "");
			}
		} catch (Exception e) {
			name = "unnamed slave";
		}
		Lapitar.log.info("Slave name is '"+name+"'");
	}
	
	@Override
	public void run() {
		System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", Boolean.toString(config.getBoolean("allowSoftware")));
		System.setProperty("org.lwjgl.opengl.Display.noinput", "true");
		try {
			Lapitar.log.info("Setting up LWJGL");
			Pbuffer test = new Pbuffer(16, 16, new PixelFormat(8, 8, 0), null);
			test.makeCurrent();
			if (!GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
				Lapitar.log.severe("Your graphics driver does not support ARB_vertex_buffer_object. The slave cannot continue.");
				test.destroy();
				return;
			}
			String glV = GL11.glGetString(GL11.GL_VERSION);
			String os = System.getProperty("os.name");
			Lapitar.log.info("OpenGL "+glV+" on "+os);
			if (os.contains("Win")) {
				Lapitar.log.severe("Lapitar does not support Windows. Continue at your own peril!");
			}
			if (!glV.contains("Mesa")) {
				Lapitar.log.warning("You are using an unsupported graphics driver.");
			}
			if (os.equals("Linux") && glV.contains("Mesa")) {
				Lapitar.log.fine("Lapitar fully supports your OS and graphics driver.");
			}
			test.destroy();
			Lapitar.log.finer("Downloading default skins");
			steve = ImageIO.read(URLUtils.constantURL("https://minecraft.net/images/steve.png"));
			alex = ImageIO.read(URLUtils.constantURL("https://minecraft.net/images/alex.png"));
			Lapitar.log.info("Connecting to RabbitMQ at "+config.getString("rabbitmq.host")+":"+config.getInt("rabbitmq.port"));
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(config.getString("rabbitmq.host"));
			factory.setPort(config.getInt("rabbitmq.port"));
			if (config.hasPath("rabbitmq.user")) {
				Lapitar.log.finer("Using authentication");
				factory.setUsername(config.getString("rabbitmq.user"));
				factory.setPassword(config.getString("rabbitmq.password"));
			}
			conn = factory.newConnection();
			Lapitar.log.info("Setting up "+config.getInt("renderers")+" render threads");
			for (int i = 0; i < config.getInt("renderers"); i++) {
				RenderThread rt = new RenderThread(this);
				threads.add(rt);
				rt.start();
			}
			channel = conn.createChannel();
			String queue = config.getString("rabbitmq.queue");
			Lapitar.log.finer("Setting up queue '"+queue+"'");
			channel.queueDeclare(queue, false, false, true, null);
			int qos = config.getInt("qos");
			if (qos != -1) {
				channel.basicQos(qos);
			}
			
			QueueingConsumer consumer = new QueueingConsumer(channel);
			Map<String, Object> args = Maps.newHashMap();
			args.put("x-priority", config.getInt("weight"));
			channel.basicConsume(queue, false, args, consumer);
			Lapitar.log.info("Listening for jobs");
			try {
				while (true) {
					try {
						Delivery delivery = consumer.nextDelivery();
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
				for (RenderThread rt : threads) {
					rt.finish();
				}
			} catch (Exception e) {
				Lapitar.log.log(Level.SEVERE, "A fatal error has occurred in the slave run loop.", e);
			}
		} catch (Exception e) {
			Lapitar.log.log(Level.SEVERE, "A fatal error has occurred while setting up the slave.", e);
		}
	}
}
