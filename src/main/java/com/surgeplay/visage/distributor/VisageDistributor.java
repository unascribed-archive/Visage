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

package com.surgeplay.visage.distributor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
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
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.surgeplay.visage.RenderMode;
import com.surgeplay.visage.Visage;
import com.surgeplay.visage.VisageRunner;
import com.surgeplay.visage.distributor.exception.NoRenderersAvailableException;
import com.surgeplay.visage.distributor.exception.RenderFailedException;
import com.surgeplay.visage.distributor.glue.HeaderHandler;
import com.surgeplay.visage.distributor.glue.LogShim;
import com.surgeplay.visage.renderer.VisageRenderer;
import com.surgeplay.visage.util.Profiles;
import com.typesafe.config.Config;

public class VisageDistributor extends Thread implements VisageRunner {
	public VisageRenderer fallback;
	public Config config;
	public Connection conn;
	public Channel channel;
	public byte[] steve, alex;
	private JedisPool pool;
	private int resolverNum, skinNum;
	private String password;
	private boolean run = true;
	public VisageDistributor(Config config) {
		super("Distributor thread");
		this.config = config;
	}
	public Jedis getResolverJedis() {
		Jedis j = getJedis();
		j.select(resolverNum);
		return j;
	}
	public Jedis getSkinJedis() {
		Jedis j = getJedis();
		j.select(skinNum);
		return j;
	}
	public Jedis getJedis() {
		Jedis j = pool.getResource();
		if (password != null) {
			j.auth(password);
		}
		return j;
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
				new File(config.getString("log")).getParentFile().mkdirs();
				server.setRequestLog(new AsyncNCSARequestLog(config.getString("log")));
			}
			GzipHandler gzip = new GzipHandler();
			gzip.setHandler(new HeaderHandler("X-Powered-By", poweredBy, resource));
			server.setHandler(gzip);
			
			String redisHost = config.getString("redis.host");
			int redisPort = config.getInt("redis.port");
			Visage.log.info("Connecting to Redis at "+redisHost+":"+redisPort);
			resolverNum = config.getInt("redis.resolver-db");
			skinNum = config.getInt("redis.skin-db");
			JedisPoolConfig jpc = new JedisPoolConfig();
			jpc.setMaxIdle(config.getInt("redis.max-idle-connections"));
			jpc.setMaxTotal(config.getInt("redis.max-total-connections"));
			jpc.setMinIdle(config.getInt("redis.min-idle-connections"));
			if (config.hasPath("redis.password")) {
				password = config.getString("redis.password");
			}
			pool = new JedisPool(jpc, redisHost, redisPort);
			
			
			Visage.log.info("Connecting to RabbitMQ at "+config.getString("rabbitmq.host")+":"+config.getInt("rabbitmq.port"));
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(config.getString("rabbitmq.host"));
			factory.setPort(config.getInt("rabbitmq.port"));
			factory.setRequestedHeartbeat(10);
			if (config.hasPath("rabbitmq.user")) {
				factory.setUsername(config.getString("rabbitmq.user"));
				factory.setPassword(config.getString("rabbitmq.password"));
			}
			String queue = config.getString("rabbitmq.queue");
			
			Closer closer = Closer.create();
			steve = ByteStreams.toByteArray(closer.register(ClassLoader.getSystemResourceAsStream("steve.png")));
			alex = ByteStreams.toByteArray(closer.register(ClassLoader.getSystemResourceAsStream("alex.png")));
			closer.close();
			
			conn = factory.newConnection();
			channel = conn.createChannel();
			if (Visage.debug) Visage.log.finer("Setting up queue '"+queue+"'");
			channel.queueDeclare(queue, false, false, true, null);
			channel.basicQos(1);
			
			if (Visage.debug) Visage.log.finer("Setting up reply queue");
			replyQueue = channel.queueDeclare().getQueue();
			consumer = new QueueingConsumer(channel);
			channel.basicConsume(replyQueue, consumer);
			
			if (config.getBoolean("renderer.enable")) {
				Visage.log.info("Starting fallback renderer");
				fallback = new VisageRenderer(config.getConfig("renderer").withValue("rabbitmq", config.getValue("rabbitmq")));
				fallback.start();
			}
			Visage.log.info("Starting Jetty");
			server.start();
			Visage.log.info("Listening for finished jobs");
			try {
				while (run) {
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
			} catch (InterruptedException e) {
			} catch (Exception e) {
				Visage.log.log(Level.SEVERE, "An unexpected error occured in the distributor run loop.", e);
				System.exit(2);
			}
			try {
				Visage.log.info("Shutting down distributor");
				server.stop();
				pool.destroy();
				conn.close(5000);
			} catch (Exception e) {
				Visage.log.log(Level.SEVERE, "A fatal error has occurred while shutting down the distributor.", e);
			}
		} catch (Exception e) {
			Visage.log.log(Level.SEVERE, "An unexpected error occured while initializing the distributor.", e);
			System.exit(1);
		}
	}
	private String replyQueue;
	private QueueingConsumer consumer;
	private Map<String, Runnable> queuedJobs = Maps.newHashMap();
	private Map<String, byte[]> responses = Maps.newHashMap();
	public RenderResponse renderRpc(RenderMode mode, int width, int height, GameProfile profile, byte[] skin, Map<String, String[]> switches) throws RenderFailedException, NoRenderersAvailableException {
		if (mode == RenderMode.SKIN) return null;
		try {
			byte[] response = null;
			String corrId = UUID.randomUUID().toString();
			BasicProperties props = new BasicProperties.Builder().correlationId(corrId).replyTo(replyQueue).build();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DeflaterOutputStream defos = new DeflaterOutputStream(baos);
			DataOutputStream dos = new DataOutputStream(defos);
			dos.writeByte(mode.ordinal());
			dos.writeShort(width);
			dos.writeShort(height);
			Profiles.writeGameProfile(dos, profile);
			dos.writeShort(switches.size());
			for (Entry<String, String[]> en : switches.entrySet()) {
				dos.writeUTF(en.getKey());
				dos.writeByte(en.getValue().length);
				for (String s : en.getValue()) {
					dos.writeUTF(s);
				}
			}
			dos.writeInt(skin.length);
			dos.write(skin);
			dos.flush();
			defos.finish();
			channel.basicPublish("", config.getString("rabbitmq.queue"), props, baos.toByteArray());
			if (Visage.debug) Visage.log.finer("Requested a "+width+"x"+height+" "+mode.name().toLowerCase()+" render for "+(profile == null ? "null" : profile.getName()));
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
			String renderer = new DataInputStream(bais).readUTF();
			int type = bais.read();
			byte[] payload = ByteStreams.toByteArray(bais);
			if (type == 0) {
				if (Visage.trace) Visage.log.finest("Got type 0, success");
				RenderResponse resp = new RenderResponse();
				resp.renderer = renderer;
				resp.png = payload;
				Visage.log.info("Receieved a "+mode.name().toLowerCase()+" render from "+resp.renderer);
				return resp;
			} else if (type == 1) {
				if (Visage.trace) Visage.log.finest("Got type 1, failure");
				ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(payload));
				Throwable t = (Throwable)ois.readObject();
				throw new RenderFailedException("Renderer reported error", t);
			} else
				throw new RenderFailedException("Malformed response from '"+renderer+"' - unknown response id "+type);
		} catch (Exception e) {
			if (e instanceof RenderFailedException)
				throw (RenderFailedException) e;
			throw new RenderFailedException("Unexpected error", e);
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
	@Override
	public void shutdown() {
		run = false;
		interrupt();
	}

}
