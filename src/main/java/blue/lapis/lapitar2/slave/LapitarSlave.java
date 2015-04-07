package blue.lapis.lapitar2.slave;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.InflaterInputStream;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.spacehq.mc.auth.GameProfile;
import org.spacehq.mc.auth.ProfileTexture;
import org.spacehq.mc.auth.ProfileTextureType;
import org.spacehq.mc.auth.SessionService;
import org.spacehq.mc.auth.properties.Property;
import org.spacehq.mc.auth.util.Base64;
import org.spacehq.mc.auth.util.URLUtils;

import blue.lapis.lapitar2.Lapitar;
import blue.lapis.lapitar2.RenderMode;
import blue.lapis.lapitar2.slave.render.Renderer;
import blue.lapis.lapitar2.util.Images;
import blue.lapis.lapitar2.util.UUIDs;

import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.typesafe.config.Config;

public class LapitarSlave extends Thread {
	private Config config;
	private Renderer[] renderers;
	private SessionService session = new SessionService();
	private Gson gson = new Gson();
	private BufferedImage steve, alex;
	private ByteArrayOutputStream png = new ByteArrayOutputStream();
	private String name;
	private Channel channel;
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
		RenderMode[] modes = RenderMode.values();
		renderers = new Renderer[modes.length];
		for (int i = 0; i < modes.length; i++) {
			renderers[i] = modes[i].newRenderer();
		}
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
				test.releaseContext();
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
			String queue = config.getString("rabbitmq.queue");
			
			Connection conn = factory.newConnection();
			channel = conn.createChannel();
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
						try {
							processDelivery(delivery);
						} catch (InterruptedException e) {
							throw e;
						} catch (Exception e) {
							Lapitar.log.log(Level.SEVERE, "An unexpected error occurred while rendering", e);
							BasicProperties props = delivery.getProperties();
							BasicProperties replyProps = new BasicProperties.Builder().correlationId(props.getCorrelationId()).build();
							ByteArrayOutputStream ex = new ByteArrayOutputStream();
							ObjectOutputStream oos = new ObjectOutputStream(ex);
							oos.writeObject(e);
							oos.flush();
							channel.basicPublish("", props.getReplyTo(), replyProps, buildResponse(1, ex.toByteArray()));
							channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
						}
					} catch (InterruptedException e) {
						break;
					}
				}
				for (Renderer r : renderers) {
					if (r != null) {
						r.destroy();
					}
				}
			} catch (Exception e) {
				Lapitar.log.log(Level.SEVERE, "A fatal error has occurred in the slave run loop.", e);
			}
		} catch (Exception e) {
			Lapitar.log.log(Level.SEVERE, "A fatal error has occurred while setting up the slave.", e);
		}
	}

	private void processDelivery(Delivery delivery) throws Exception {
		BasicProperties props = delivery.getProperties();
		BasicProperties replyProps = new BasicProperties.Builder().correlationId(props.getCorrelationId()).build();
		DataInputStream data = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(delivery.getBody())));
		RenderMode mode = RenderMode.values()[data.readUnsignedByte()];
		int width = data.readUnsignedShort();
		int height = data.readUnsignedShort();
		int supersampling = data.readUnsignedByte();
		GameProfile profile = readGameProfile(data);
		Lapitar.log.finer("Rendering a "+width+"x"+height+" "+mode.name().toLowerCase()+" ("+supersampling+"x supersampling) for "+profile.getName());
		byte[] pngBys = draw(mode, width, height, supersampling, profile);
		Lapitar.log.finest("Got png bytes");
		channel.basicPublish("", props.getReplyTo(), replyProps, buildResponse(0, pngBys));
		Lapitar.log.finest("Published response");
		channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
		Lapitar.log.finest("Ack'd message");
	}

	private byte[] buildResponse(int type, byte[] payload) throws IOException {
		Lapitar.log.finest("Building response of type "+type);
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		new DataOutputStream(result).writeUTF(name);
		result.write(type);
		result.write(payload);
		byte[] resp = result.toByteArray();
		Lapitar.log.finest("Built - "+resp.length+" bytes long");
		return resp;
	}

	public byte[] draw(RenderMode mode, int width, int height, int supersampling, GameProfile profile) throws Exception {
		png.reset();
		Lapitar.log.finest("Reset png");
		Map<ProfileTextureType, ProfileTexture> tex = session.getTextures(profile, false);
		boolean slim = isSlim(profile);
		BufferedImage skin;
		//BufferedImage cape;
		BufferedImage out;
		if (tex.containsKey(ProfileTextureType.SKIN)) {
			skin = ImageIO.read(new URL(tex.get(ProfileTextureType.SKIN).getUrl()));
		} else {
			skin = slim ? alex : steve;
		}
		Lapitar.log.finest("Got skin");
		Lapitar.log.finest(mode.name());
		switch (mode) {
			case FACE:
				width /= supersampling;
				height /= supersampling;
				out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				int border = width/24;
				Image face = skin.getSubimage(8, 8, 8, 8).getScaledInstance(width-(border*2), height-(border*2), Image.SCALE_FAST);
				Image helm = skin.getSubimage(40, 8, 8, 8).getScaledInstance(width, height, Image.SCALE_FAST);
				Graphics2D g2d = out.createGraphics();
				g2d.drawImage(face, border, border, null);
				g2d.drawImage(helm, 0, 0, null);
				g2d.dispose();
				break;
			case SKIN:
				out = skin;
				break;
			default: {
				Renderer renderer = renderers[mode.ordinal()];
				if (!renderer.isInitialized()) {
					Lapitar.log.finest("Initialized renderer");
					renderer.init(supersampling);
				}
				try {
					Lapitar.log.finest("Rendering");
					renderer.render(width, height);
					Lapitar.log.finest("Rendered - reading pixels");
					GL11.glReadBuffer(GL11.GL_FRONT);
					ByteBuffer buf = BufferUtils.createByteBuffer(width * height * 4);
					GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buf);
					BufferedImage img  = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
					int[] pixels = new int[width*height];
					buf.asIntBuffer().get(pixels);
					img.setRGB(0, 0, width, height, pixels, 0, width);
					Lapitar.log.finest("Read pixels");
					out = Images.toBuffered(img.getScaledInstance(width/supersampling, height/supersampling, Image.SCALE_AREA_AVERAGING));
					Lapitar.log.finest("Rescaled image");
				} finally {
					renderer.finish();
					Lapitar.log.finest("Finished renderer");
				}
				break;
			}
		}
		ImageIO.write(out, "PNG", png);
		Lapitar.log.finest("Wrote png");
		return png.toByteArray();
	}

	private boolean isSlim(GameProfile profile) throws IOException {
		String texJson = new String(Base64.decode(profile.getProperties().get("textures").getValue().getBytes(StandardCharsets.UTF_8)));
		JsonObject obj = gson.fromJson(texJson, JsonObject.class);
		JsonObject tex = obj.getAsJsonObject("textures");
		if (tex.has("SKIN")) {
			JsonObject skin = tex.getAsJsonObject("SKIN");
			if (skin.has("metadata")) {
				if ("slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString()))
					return true;
			}
			return false;
		}
		return UUIDs.isAlex(profile.getId());
	}

	private GameProfile readGameProfile(DataInputStream data) throws IOException {
		UUID uuid = new UUID(data.readLong(), data.readLong());
		String name = data.readUTF();
		GameProfile profile = new GameProfile(uuid, name);
		int len = data.readUnsignedShort();
		for (int i = 0; i < len; i++) {
			boolean signed = data.readBoolean();
			Property prop;
			if (signed) {
				prop = new Property(data.readUTF(), data.readUTF(), data.readUTF());
			} else {
				prop = new Property(data.readUTF(), data.readUTF());
			}
			profile.getProperties().put(data.readUTF(), prop);
		}
		return profile;
	}
}
