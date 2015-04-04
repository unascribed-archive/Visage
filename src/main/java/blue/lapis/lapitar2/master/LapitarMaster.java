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

import java.net.InetSocketAddress;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.jetty.server.AsyncNCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.log.Log;

import blue.lapis.lapitar2.Lapitar;
import blue.lapis.lapitar2.slave.LapitarSlave;

import com.typesafe.config.Config;

public class LapitarMaster extends Thread {
	protected LapitarSlave fallback;
	protected Config config;
	public LapitarMaster(Config config) {
		super("Master thread");
		this.config = config;
	}
	@Override
	public void run() {
		try {
			Log.setLog(new LogShim(Lapitar.log));
			
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
			server.start();
			if (config.getBoolean("slave.enable")) {
				fallback = new LapitarSlave(config.getConfig("slave").withValue("rabbitmq", config.getValue("rabbitmq")));
				fallback.start();
			}
		} catch (Exception e) {
			Lapitar.log.log(Level.SEVERE, "", e);
		}
	}

}
