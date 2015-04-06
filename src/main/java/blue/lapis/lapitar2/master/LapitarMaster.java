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
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.zip.DeflaterOutputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.AsyncNCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.log.Log;
import org.spacehq.mc.auth.GameProfile;
import org.spacehq.mc.auth.properties.Property;

import blue.lapis.lapitar2.Lapitar;
import blue.lapis.lapitar2.RenderMode;
import blue.lapis.lapitar2.slave.LapitarSlave;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.ShutdownSignalException;
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
			String queue = config.getString("rabbitmq.queue");
			
			conn = factory.newConnection();
			channel = conn.createChannel();
			Lapitar.log.info("Setting up queue '"+queue+"'");
			channel.queueDeclare(queue, false, false, true, null);
			channel.basicQos(1);
			
			Lapitar.log.info("Setting up reply queue");
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
		} catch (Exception e) {
			Lapitar.log.log(Level.SEVERE, "A fatal error has occured while initializing the master. Lapitar cannot continue.", e);
		}
	}
	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private String replyQueue;
	private QueueingConsumer consumer;
	public RenderResponse renderRpc(RenderMode mode, int width, int height, int supersampling, GameProfile profile) throws RenderFailedException {
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
			Lapitar.log.info("Requested a "+width+"x"+height+" "+mode.name().toLowerCase()+" render ("+supersampling+"x supersampling) for "+profile.getName());
			while (true) {
				try {
					Delivery delivery = consumer.nextDelivery(config.getDuration("render.timeout", TimeUnit.MILLISECONDS));
					if (delivery == null)
						throw new RenderFailedException("Request timed out");
					if (corrId.equals(delivery.getProperties().getCorrelationId())) {
						Lapitar.log.info("Got response");
						response = delivery.getBody();
						channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
						break;
					} else {
						Lapitar.log.warning("Incorrect correlation ID?");
						channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
					}
				} catch (ShutdownSignalException e) {
					throw new RenderFailedException("RabbitMQ shut down", e);
				} catch (ConsumerCancelledException e) {
					throw new RenderFailedException("Consumer cancelled", e);
				} catch (InterruptedException e) {
					throw new RenderFailedException("Request interrupted", e);
				}
			}
			RenderResponse resp = new RenderResponse();
			ByteArrayInputStream bais = new ByteArrayInputStream(response);
			resp.slave = new DataInputStream(bais).readUTF();
			resp.png = IOUtils.toByteArray(bais);
			Lapitar.log.info("Receieved render from "+resp.slave);
			return resp;
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
