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

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;

import org.eclipse.jetty.server.AsyncNCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Lapitar {
	public static final String VERSION = "2.0.0";
	
	public static final Config config = ConfigFactory.parseFile(new File("conf/master.conf"));
	public static void main(String[] args) throws Exception {
		System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", config.getString("slave.allowSoftware"));
		System.setProperty("org.lwjgl.opengl.Display.noinput", "true");
		
		Server server = new Server(new InetSocketAddress(config.getString("jetty.bind"), config.getInt("jetty.port")));
		
		List<String> expose = Lapitar.config.getStringList("expose");
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
		resource.setResourceBase(Lapitar.config.getString("jetty.static"));
		resource.setDirectoriesListed(false);
		resource.setWelcomeFiles(new String[] {"index.html"});
		resource.setHandler(new LapitarHandler());

		if (!"/dev/null".equals(Lapitar.config.getString("log"))) {
			server.setRequestLog(new AsyncNCSARequestLog(Lapitar.config.getString("log")));
		}
		server.setHandler(new HeaderHandler("X-Powered-By", poweredBy, resource));
		server.start();
		server.join(); // TODO
	}

}
