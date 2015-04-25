package com.gameminers.visage.slave;

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
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.InflaterInputStream;

import javax.imageio.ImageIO;

import org.spacehq.mc.auth.GameProfile;
import org.spacehq.mc.auth.ProfileTexture;
import org.spacehq.mc.auth.ProfileTextureType;
import org.spacehq.mc.auth.properties.Property;
import org.spacehq.mc.auth.util.Base64;

import com.gameminers.visage.Visage;
import com.gameminers.visage.RenderMode;
import com.gameminers.visage.slave.render.Renderer;
import com.gameminers.visage.util.UUIDs;
import com.google.gson.JsonObject;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.QueueingConsumer.Delivery;

public class RenderThread extends Thread {
	private static int nextId = 1;
	private VisageSlave parent;
	private Renderer[] renderers;
	private boolean run = true;
	private Deque<Delivery> toProcess = new ArrayDeque<>();
	public RenderThread(VisageSlave parent) {
		super("Render thread #"+(nextId++));
		this.parent = parent;
		RenderMode[] modes = RenderMode.values();
		renderers = new Renderer[modes.length];
		for (int i = 0; i < modes.length; i++) {
			renderers[i] = modes[i].newRenderer();
		}
	}
	
	@Override
	public void run() {
		try {
			Visage.log.info("Waiting for jobs");
			try {
				while (run) {
					if (!toProcess.isEmpty()) {
						Delivery delivery = toProcess.pop();
						try {
							processDelivery(delivery);
						} catch (Exception e) {
							Visage.log.log(Level.SEVERE, "An unexpected error occurred while rendering", e);
							BasicProperties props = delivery.getProperties();
							BasicProperties replyProps = new BasicProperties.Builder().correlationId(props.getCorrelationId()).build();
							ByteArrayOutputStream ex = new ByteArrayOutputStream();
							ObjectOutputStream oos = new ObjectOutputStream(ex);
							oos.writeObject(e);
							oos.flush();
							parent.channel.basicPublish("", props.getReplyTo(), replyProps, buildResponse(1, ex.toByteArray()));
							parent.channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
						}
					} else {
						synchronized (toProcess) {
							toProcess.wait();
						}
					}
				}
				for (Renderer r : renderers) {
					if (r != null) {
						r.destroy();
					}
				}
			} catch (Exception e) {
				Visage.log.log(Level.SEVERE, "A fatal error has occurred in the render thread run loop.", e);
			}
		} catch (Exception e) {
			Visage.log.log(Level.SEVERE, "A fatal error has occurred while setting up a render thread.", e);
		}
	}

	public void process(Delivery delivery) throws IOException {
		toProcess.addLast(delivery);
		synchronized (toProcess) {
			toProcess.notify();
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
		if (Visage.debug) Visage.log.finer("Rendering a "+width+"x"+height+" "+mode.name().toLowerCase()+" ("+supersampling+"x supersampling) for "+(profile == null ? "null" : profile.getName()));
		byte[] pngBys = draw(mode, width, height, supersampling, profile);
		if (Visage.trace) Visage.log.finest("Got png bytes");
		parent.channel.basicPublish("", props.getReplyTo(), replyProps, buildResponse(0, pngBys));
		if (Visage.trace) Visage.log.finest("Published response");
		parent.channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
		if (Visage.trace) Visage.log.finest("Ack'd message");
	}

	private byte[] buildResponse(int type, byte[] payload) throws IOException {
		if (Visage.trace) Visage.log.finest("Building response of type "+type);
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		new DataOutputStream(result).writeUTF(parent.name);
		result.write(type);
		result.write(payload);
		byte[] resp = result.toByteArray();
		if (Visage.trace) Visage.log.finest("Built - "+resp.length+" bytes long");
		return resp;
	}

	public byte[] draw(RenderMode mode, int width, int height, int supersampling, GameProfile profile) throws Exception {
		Map<ProfileTextureType, ProfileTexture> tex = parent.session.getTextures(profile, false);
		boolean slim = isSlim(profile);
		BufferedImage skin;
		//BufferedImage cape;
		BufferedImage out;
		if (tex.containsKey(ProfileTextureType.SKIN)) {
			skin = ImageIO.read(new URL(tex.get(ProfileTextureType.SKIN).getUrl()));
		} else {
			skin = slim ? parent.alex : parent.steve;
		}
		if (skin.getHeight() == 32) {
			if (Visage.debug) Visage.log.finer("Skin is legacy; painting onto new-style canvas");
			BufferedImage canvas = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = canvas.createGraphics();
			g.drawImage(skin, 0, 0, null);
			g.drawImage(flipLimb(skin.getSubimage(0, 16, 16, 16)), 16, 48, null);
			g.drawImage(flipLimb(skin.getSubimage(40, 16, 16, 16)), 32, 48, null);
			g.dispose();
			skin = canvas;
		}
		if (Visage.trace) Visage.log.finest("Got skin");
		if (Visage.trace) Visage.log.finest(mode.name());
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
					if (Visage.trace) Visage.log.finest("Initialized renderer");
					renderer.init(supersampling);
				}
				try {
					if (Visage.trace) Visage.log.finest("Uploading");
					renderer.setSkin(skin);
					if (Visage.trace) Visage.log.finest("Rendering");
					renderer.render(width, height);
					if (Visage.trace) Visage.log.finest("Rendered - reading pixels");
					out = renderer.readPixels(width, height);
					if (Visage.trace) Visage.log.finest("Rescaled image");
				} finally {
					renderer.finish();
					if (Visage.trace) Visage.log.finest("Finished renderer");
				}
				break;
			}
		}
		ByteArrayOutputStream png = new ByteArrayOutputStream();
		ImageIO.write(out, "PNG", png);
		if (Visage.trace) Visage.log.finest("Wrote png");
		return png.toByteArray();
	}

	private BufferedImage flipLimb(BufferedImage in) {
		BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), in.getType());
		
		BufferedImage front = flipHorziontally(in.getSubimage(4, 4, 4, 12));
		BufferedImage back = flipHorziontally(in.getSubimage(12, 4, 4, 12));
		
		BufferedImage top = flipHorziontally(in.getSubimage(4, 0, 4, 4));
		BufferedImage bottom = flipHorziontally(in.getSubimage(8, 0, 4, 4));
		
		BufferedImage left = in.getSubimage(8, 4, 4, 12);
		BufferedImage right = in.getSubimage(0, 4, 4, 12);
		
		Graphics2D g = out.createGraphics();
		g.drawImage(front, 4, 4, null);
		g.drawImage(back, 12, 4, null);
		g.drawImage(top, 4, 0, null);
		g.drawImage(bottom, 8, 0, null);
		g.drawImage(left, 0, 4, null); // left goes to right
		g.drawImage(right, 8, 4, null); // right goes to left
		g.dispose();
		return out;
	}
	
	private BufferedImage flipHorziontally(BufferedImage in) {
		BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), in.getType());
		Graphics2D g = out.createGraphics();
		g.drawImage(in, 0, 0, in.getWidth(), in.getHeight(), in.getWidth(), 0, 0, in.getHeight(), null);
		g.dispose();
		return out;
	}
	
	private boolean isSlim(GameProfile profile) throws IOException {
		if (profile.getProperties().containsKey("textures")) {
			String texJson = new String(Base64.decode(profile.getProperties().get("textures").getValue().getBytes(StandardCharsets.UTF_8)));
			JsonObject obj = parent.gson.fromJson(texJson, JsonObject.class);
			JsonObject tex = obj.getAsJsonObject("textures");
			if (tex.has("SKIN")) {
				JsonObject skin = tex.getAsJsonObject("SKIN");
				if (skin.has("metadata")) {
					if ("slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString()))
						return true;
				}
				return false;
			}
		}
		return UUIDs.isAlex(profile.getId());
	}

	private GameProfile readGameProfile(DataInputStream data) throws IOException {
		boolean present = data.readBoolean();
		if (!present)
			return new GameProfile(new UUID(0, 0), "<unknown>");
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

	public void finish() {
		run = false;
		interrupt();
	}

}
