package blue.lapis.lapitar2.slave;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.InflaterInputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.Charsets;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.spacehq.mc.auth.GameProfile;
import org.spacehq.mc.auth.ProfileTexture;
import org.spacehq.mc.auth.ProfileTextureType;
import org.spacehq.mc.auth.SessionService;
import org.spacehq.mc.auth.properties.Property;
import org.spacehq.mc.auth.util.Base64;
import org.spacehq.mc.auth.util.URLUtils;

import blue.lapis.lapitar2.Images;
import blue.lapis.lapitar2.Lapitar;
import blue.lapis.lapitar2.RenderMode;
import blue.lapis.lapitar2.UUIDs;
import blue.lapis.lapitar2.slave.render.Renderer;

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
	private boolean run = true;
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
		} catch (Exception e) {
			name = "unnamed slave";
		}
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
			Class.forName("org.lwjgl.opengl.Display");
			Lapitar.log.info("Downloading default skins");
			steve = ImageIO.read(URLUtils.constantURL("https://minecraft.net/images/steve.png"));
			alex = ImageIO.read(URLUtils.constantURL("https://minecraft.net/images/alex.png"));
			Lapitar.log.info("Connecting to RabbitMQ at "+config.getString("rabbitmq.host")+":"+config.getInt("rabbitmq.port"));
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(config.getString("rabbitmq.host"));
			factory.setPort(config.getInt("rabbitmq.port"));
			String queue = config.getString("rabbitmq.queue");
			
			Connection conn = factory.newConnection();
			channel = conn.createChannel();
			Lapitar.log.info("Setting up queue '"+queue+"'");
			channel.queueDeclare(queue, false, false, true, null);
			channel.basicQos(1);
			
			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(queue, consumer);
			Lapitar.log.info("Listening for jobs");
			try {
				while (run) {
					try {
						Delivery delivery = consumer.nextDelivery();
						try {
							processDelivery(delivery);
						} catch (Exception e) {
							Lapitar.log.log(Level.SEVERE, "An unexpected error occurred while rendering", e);
							channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
						}
					} catch (InterruptedException e) {
						run = false;
						break;
					}
				}
				for (Renderer r : renderers) {
					if (r != null) {
						r.destroy();
					}
				}
			} catch (Exception e) {
				Lapitar.log.log(Level.SEVERE, "A fatal error has occurred in the slave run loop. Lapitar cannot continue.", e);
			}
		} catch (Exception e) {
			Lapitar.log.log(Level.SEVERE, "A fatal error has occurred while setting up the slave. Lapitar cannot continue.", e);
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
		Lapitar.log.info("Rendering a "+width+"x"+height+" "+mode.name().toLowerCase()+" ("+supersampling+"x supersampling) for "+profile.getName());
		byte[] pngBys = draw(mode, width, height, supersampling, profile);
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		new DataOutputStream(result).writeUTF(name);
		result.write(pngBys);
		channel.basicPublish("", props.getReplyTo(), replyProps, result.toByteArray());
		channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
	}

	public byte[] draw(RenderMode mode, int width, int height, int supersampling, GameProfile profile) throws Exception {
		png.reset();
		Map<ProfileTextureType, ProfileTexture> tex = session.getTextures(profile, false);
		boolean slim = isSlim(profile);
		BufferedImage skin, cape, out;
		if (tex.containsKey(ProfileTextureType.SKIN)) {
			skin = ImageIO.read(new URL(tex.get(ProfileTextureType.SKIN).getUrl()));
		} else {
			skin = slim ? alex : steve;
		}
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
					renderer.init(supersampling);
				}
				try {
					renderer.render(width, height);
					GL11.glReadBuffer(GL11.GL_FRONT);
					ByteBuffer buf = BufferUtils.createByteBuffer(width * height * 4);
					GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buf);
					BufferedImage img  = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
					int[] pixels = new int[width*height];
					buf.asIntBuffer().get(pixels);
					img.setRGB(0, 0, width, height, pixels, 0, width);
					out = Images.toBuffered(img.getScaledInstance(width/supersampling, height/supersampling, Image.SCALE_AREA_AVERAGING));
				} finally {
					renderer.finish();
				}
				break;
			}
		}
		ImageIO.write(out, "PNG", png);
		return png.toByteArray();
	}

	private boolean isSlim(GameProfile profile) throws IOException {
		String texJson = new String(Base64.decode(profile.getProperties().get("textures").getValue().getBytes(Charsets.UTF_8)));
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
