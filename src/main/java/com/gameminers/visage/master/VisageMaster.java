/*
 * Visage2
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
package com.gameminers.visage.master;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.zip.DeflaterOutputStream;

import org.eclipse.jetty.server.AsyncNCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.log.Log;
import org.spacehq.mc.auth.GameProfile;
import org.spacehq.mc.auth.properties.Property;

import com.gameminers.visage.Visage;
import com.gameminers.visage.RenderMode;
import com.gameminers.visage.master.cache.CacheManager;
import com.gameminers.visage.master.cache.GuavaBasicCacheManager;
import com.gameminers.visage.master.exception.NoSlavesAvailableException;
import com.gameminers.visage.master.exception.RenderFailedException;
import com.gameminers.visage.master.glue.HeaderHandler;
import com.gameminers.visage.master.glue.LogShim;
import com.gameminers.visage.slave.VisageSlave;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.typesafe.config.Config;

public class VisageMaster extends Thread {
	public VisageSlave fallback;
	public Config config;
	public Connection conn;
	public Channel channel;
	public CacheManager cache;
	public VisageMaster(Config config) {
		super("Master thread");
		this.config = config;
	}
	@Override
	public void run() {
		try {
			Log.setLog(new LogShim(Visage.log));
			long total = Runtime.getRuntime().totalMemory();
			long max = Runtime.getRuntime().maxMemory();
			if (Visage.debug) Visage.log.finer("Current heap size: "+humanReadableByteCount(total, false));
			if (Visage.debug) Visage.log.finer("Max heap size: "+humanReadableByteCount(max, false));
			if (total < max) {
				Visage.log.warning("You have set your minimum heap size (Xms) lower than the maximum heap size (Xmx) - this can cause GC thrashing. It is strongly recommended to set them both to the same value.");
			}
			if (max < (1000*1000*1000)) {
				Visage.log.warning("The heap size (Xmx) is less than one gigabyte; it is recommended to run Visage with a gigabyte or more. Use -Xms1G and -Xmx1G to do this.");
			}
			Visage.log.info("Initializing cache");
			// TODO
			//if (!config.getBoolean("cache.enabled")) {
				cache = new GuavaBasicCacheManager();
			//} else {
			//	cache = new JCSCacheManager(config.getConfig("cache"));
			//}
			Visage.log.info("Setting up Jetty");
			Server server = new Server(new InetSocketAddress(config.getString("http.bind"), config.getInt("http.port")));
			
			List<String> expose = config.getStringList("expose");
			String poweredBy;
			if (expose.contains("server")) {
				if (expose.contains("version")) {
					poweredBy = "Visage v"+Visage.VERSION;
				} else {
					poweredBy = "Visage";
				}
			} else {
				poweredBy = null;
			}
			
			ResourceHandler resource = new ResourceHandler();
			resource.setResourceBase(config.getString("http.static"));
			resource.setDirectoriesListed(false);
			resource.setWelcomeFiles(new String[] {"index.html"});
			resource.setHandler(new VisageHandler(this));
	
			if (!"/dev/null".equals(config.getString("log"))) {
				server.setRequestLog(new AsyncNCSARequestLog(config.getString("log")));
			}
			GzipHandler gzip = new GzipHandler();
			gzip.setHandler(new HeaderHandler("X-Powered-By", poweredBy, resource));
			server.setHandler(gzip);
			
			Visage.log.info("Connecting to RabbitMQ at "+config.getString("rabbitmq.host")+":"+config.getInt("rabbitmq.port"));
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(config.getString("rabbitmq.host"));
			factory.setPort(config.getInt("rabbitmq.port"));
			if (config.hasPath("rabbitmq.user")) {
				factory.setUsername(config.getString("rabbitmq.user"));
				factory.setPassword(config.getString("rabbitmq.password"));
			}
			String queue = config.getString("rabbitmq.queue");
			
			conn = factory.newConnection();
			channel = conn.createChannel();
			if (Visage.debug) Visage.log.finer("Setting up queue '"+queue+"'");
			channel.queueDeclare(queue, false, false, true, null);
			channel.basicQos(1);
			
			if (Visage.debug) Visage.log.finer("Setting up reply queue");
			replyQueue = channel.queueDeclare().getQueue();
			consumer = new QueueingConsumer(channel);
			channel.basicConsume(replyQueue, consumer);
			
			if (config.getBoolean("slave.enable")) {
				Visage.log.info("Starting fallback slave");
				fallback = new VisageSlave(config.getConfig("slave").withValue("rabbitmq", config.getValue("rabbitmq")));
				fallback.start();
			}
			Visage.log.info("Starting Jetty");
			server.start();
			Visage.log.info("Listening for finished jobs");
			try {
				while (true) {
					Delivery delivery = consumer.nextDelivery();
					if (Visage.trace) Visage.log.finest("Got delivery");
					try {
						String corrId = delivery.getProperties().getCorrelationId();
						if (queuedJobs.containsKey(corrId)) {
							if (Visage.trace) Visage.log.finest("Valid");
							responses.put(corrId, delivery.getBody());
							Runnable run = queuedJobs.get(corrId);
							queuedJobs.remove(corrId);
							if (Visage.trace) Visage.log.finest("Removed from queue");
							run.run();
							if (Visage.trace) Visage.log.finest("Ran runnable");
							channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
							if (Visage.trace) Visage.log.finest("Ack'd");
						} else {
							Visage.log.warning("Unknown correlation ID?");
							channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
						}
					} catch (Exception e) {
						Visage.log.log(Level.WARNING, "An unexpected error occured while attempting to process a response.", e);
					}
				}
			} catch (Exception e) {
				Visage.log.log(Level.SEVERE, "An unexpected error occured in the master run loop.", e);
				System.exit(2);
			}
		} catch (Exception e) {
			Visage.log.log(Level.SEVERE, "An unexpected error occured while initializing the master.", e);
			System.exit(1);
		}
	}
	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private String replyQueue;
	private QueueingConsumer consumer;
	private Map<String, Runnable> queuedJobs = Maps.newHashMap();
	private Map<String, byte[]> responses = Maps.newHashMap();
	public RenderResponse renderRpc(RenderMode mode, int width, int height, int supersampling, GameProfile profile, Map<String, String[]> switches) throws RenderFailedException, NoSlavesAvailableException {
		baos.reset();
		try {
			byte[] response = null;
			String corrId = UUID.randomUUID().toString();
			BasicProperties props = new BasicProperties.Builder().correlationId(corrId).replyTo(replyQueue).build();
			DeflaterOutputStream defos = new DeflaterOutputStream(baos);
			DataOutputStream dos = new DataOutputStream(defos);
			dos.writeByte(mode.ordinal());
			dos.writeShort(width);
			dos.writeShort(height);
			dos.writeByte(supersampling);
			writeGameProfile(dos, profile);
			dos.writeShort(switches.size());
			for (Entry<String, String[]> en : switches.entrySet()) {
				dos.writeUTF(en.getKey());
				dos.writeShort(en.getValue().length);
				for (String s : en.getValue()) {
					dos.writeUTF(s);
				}
			}
			dos.flush();
			defos.finish();
			channel.basicPublish("", config.getString("rabbitmq.queue"), props, baos.toByteArray());
			if (Visage.debug) Visage.log.finer("Requested a "+width+"x"+height+" "+mode.name().toLowerCase()+" render ("+supersampling+"x supersampling) for "+(profile == null ? "null" : profile.getName()));
			final Object waiter = new Object();
			queuedJobs.put(corrId, new Runnable() {
				@Override
				public void run() {
					if (Visage.debug) Visage.log.finer("Got response");
					synchronized (waiter) {
						waiter.notify();
					}
				}
			});
			long start = System.currentTimeMillis();
			long timeout = config.getDuration("render.timeout", TimeUnit.MILLISECONDS);
			synchronized (waiter) {
				while (queuedJobs.containsKey(corrId) && (System.currentTimeMillis()-start) < timeout) {
					if (Visage.trace) Visage.log.finest("Waiting...");
					waiter.wait(timeout);
				}
			}
			if (queuedJobs.containsKey(corrId)) {
				if (Visage.trace) Visage.log.finest("Queue still contains this request, assuming timeout");
				queuedJobs.remove(corrId);
				throw new RenderFailedException("Request timed out");
			}
			response = responses.get(corrId);
			responses.remove(corrId);
			if (response == null)
				throw new RenderFailedException("Response was null");
			ByteArrayInputStream bais = new ByteArrayInputStream(response);
			String slave = new DataInputStream(bais).readUTF();
			int type = bais.read();
			byte[] payload = ByteStreams.toByteArray(bais);
			if (type == 0) {
				if (Visage.trace) Visage.log.finest("Got type 0, success");
				RenderResponse resp = new RenderResponse();
				resp.slave = slave;
				resp.png = payload;
				if (Visage.debug) Visage.log.finer("Receieved render from "+resp.slave);
				return resp;
			} else if (type == 1) {
				if (Visage.trace) Visage.log.finest("Got type 1, failure");
				ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(payload));
				Throwable t = (Throwable)ois.readObject();
				throw new RenderFailedException("Slave reported error", t);
			} else
				throw new RenderFailedException("Malformed response from '"+slave+"' - unknown response id "+type);
		} catch (Exception e) {
			if (e instanceof RenderFailedException)
				throw (RenderFailedException) e;
			throw new RenderFailedException("Unexpected error", e);
		}
	}
	
	private void writeGameProfile(DataOutputStream data, GameProfile profile) throws IOException {
		if (profile == null) {
			data.writeBoolean(false);
			return;
		}
		data.writeBoolean(true);
		data.writeLong(profile.getId().getMostSignificantBits());
		data.writeLong(profile.getId().getLeastSignificantBits());
		data.writeUTF(profile.getName());
		data.writeShort(profile.getProperties().size());
		for (Entry<String, Property> en : profile.getProperties().entrySet()) {
			data.writeBoolean(en.getValue().hasSignature());
			if (en.getValue().hasSignature()) {
				data.writeUTF(en.getValue().getName());
				data.writeUTF(en.getValue().getValue());
				data.writeUTF(en.getValue().getSignature());
			} else {
				data.writeUTF(en.getValue().getName());
				data.writeUTF(en.getValue().getValue());
			}
			data.writeUTF(en.getKey());
		}
	}
	
	/**
	 * <a href="http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java">Source</a>
	 * @author aioobe
	 */
	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

}
