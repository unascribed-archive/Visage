/*
 * Lapitar2
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
package blue.lapis.lapitar2.master;

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
import org.eclipse.jetty.util.log.Log;
import org.spacehq.mc.auth.GameProfile;
import org.spacehq.mc.auth.properties.Property;

import blue.lapis.lapitar2.Lapitar;
import blue.lapis.lapitar2.RenderMode;
import blue.lapis.lapitar2.master.exception.NoSlavesAvailableException;
import blue.lapis.lapitar2.master.exception.RenderFailedException;
import blue.lapis.lapitar2.master.glue.HeaderHandler;
import blue.lapis.lapitar2.master.glue.LogShim;
import blue.lapis.lapitar2.slave.LapitarSlave;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.typesafe.config.Config;

public class LapitarMaster extends Thread {
	public LapitarSlave fallback;
	public Config config;
	public Connection conn;
	public Channel channel;
	public LapitarMaster(Config config) {
		super("Master thread");
		this.config = config;
	}
	@Override
	public void run() {
		try {
			Log.setLog(new LogShim(Lapitar.log));
			Lapitar.log.info("Setting up Jetty");
			Server server = new Server(new InetSocketAddress(config.getString("jetty.bind"), config.getInt("jetty.port")));
			
			List<String> expose = config.getStringList("expose");
			String poweredBy;
			if (expose.contains("server")) {
				if (expose.contains("version")) {
					poweredBy = "Lapitar v"+Lapitar.VERSION;
				} else {
					poweredBy = "Lapitar";
				}
			} else {
				poweredBy = null;
			}
			
			ResourceHandler resource = new ResourceHandler();
			resource.setResourceBase(config.getString("jetty.static"));
			resource.setDirectoriesListed(false);
			resource.setWelcomeFiles(new String[] {"index.html"});
			resource.setHandler(new LapitarHandler(this));
	
			if (!"/dev/null".equals(config.getString("log"))) {
				server.setRequestLog(new AsyncNCSARequestLog(config.getString("log")));
			}
			server.setHandler(new HeaderHandler("X-Powered-By", poweredBy, resource));
			
			Lapitar.log.info("Connecting to RabbitMQ at "+config.getString("rabbitmq.host")+":"+config.getInt("rabbitmq.port"));
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
			Lapitar.log.finer("Setting up queue '"+queue+"'");
			channel.queueDeclare(queue, false, false, true, null);
			channel.basicQos(1);
			
			Lapitar.log.finer("Setting up reply queue");
			replyQueue = channel.queueDeclare().getQueue();
			consumer = new QueueingConsumer(channel);
			channel.basicConsume(replyQueue, consumer);
			
			if (config.getBoolean("slave.enable")) {
				Lapitar.log.info("Starting fallback slave");
				fallback = new LapitarSlave(config.getConfig("slave").withValue("rabbitmq", config.getValue("rabbitmq")));
				fallback.start();
			}
			Lapitar.log.info("Starting Jetty");
			server.start();
			Lapitar.log.info("Listening for finished jobs");
			try {
				while (true) {
					Delivery delivery = consumer.nextDelivery();
					Lapitar.log.finer("Got delivery");
					try {
						String corrId = delivery.getProperties().getCorrelationId();
						if (queuedJobs.containsKey(corrId)) {
							Lapitar.log.finer("Valid");
							responses.put(corrId, delivery.getBody());
							Runnable run = queuedJobs.get(corrId);
							queuedJobs.remove(corrId);
							Lapitar.log.finer("Removed from queue");
							run.run();
							Lapitar.log.finer("Ran runnable");
							channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
							Lapitar.log.finer("Ack'd");
						} else {
							Lapitar.log.warning("Unknown correlation ID?");
							channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
						}
					} catch (Exception e) {
						Lapitar.log.log(Level.WARNING, "An unexpected error occured while attempting to process a response.", e);
					}
				}
			} catch (Exception e) {
				Lapitar.log.log(Level.SEVERE, "An unexpected error occured in the master run loop.", e);
				System.exit(2);
			}
		} catch (Exception e) {
			Lapitar.log.log(Level.SEVERE, "An unexpected error occured while initializing the master.", e);
			System.exit(1);
		}
	}
	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private String replyQueue;
	private QueueingConsumer consumer;
	private Map<String, Runnable> queuedJobs = Maps.newHashMap();
	private Map<String, byte[]> responses = Maps.newHashMap();
	public RenderResponse renderRpc(RenderMode mode, int width, int height, int supersampling, GameProfile profile) throws RenderFailedException, NoSlavesAvailableException {
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
			dos.flush();
			defos.finish();
			channel.basicPublish("", config.getString("rabbitmq.queue"), props, baos.toByteArray());
			Lapitar.log.finer("Requested a "+width+"x"+height+" "+mode.name().toLowerCase()+" render ("+supersampling+"x supersampling) for "+profile.getName());
			final Object waiter = new Object();
			queuedJobs.put(corrId, new Runnable() {
				@Override
				public void run() {
					Lapitar.log.finer("Got response");
					synchronized (waiter) {
						waiter.notify();
					}
				}
			});
			long start = System.currentTimeMillis();
			long timeout = config.getDuration("render.timeout", TimeUnit.MILLISECONDS);
			synchronized (waiter) {
				while (queuedJobs.containsKey(corrId) && (System.currentTimeMillis()-start) < timeout) {
					Lapitar.log.finer("Waiting...");
					waiter.wait(timeout);
				}
			}
			if (queuedJobs.containsKey(corrId)) {
				Lapitar.log.finer("Queue still contains this request, assuming timeout");
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
				Lapitar.log.finer("Got type 0, success");
				RenderResponse resp = new RenderResponse();
				resp.slave = slave;
				resp.png = payload;
				Lapitar.log.finer("Receieved render from "+resp.slave);
				return resp;
			} else if (type == 1) {
				Lapitar.log.finer("Got type 1, failure");
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

}
