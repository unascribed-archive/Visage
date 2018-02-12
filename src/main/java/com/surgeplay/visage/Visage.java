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

package com.surgeplay.visage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fusesource.jansi.AnsiConsole;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.surgeplay.visage.distributor.VisageDistributor;
import com.surgeplay.visage.renderer.VisageRenderer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Visage {
	public static final String VERSION = System.getProperty("com.surgeplay.visage.internal.version", "2.0.0");
	public static final Formatter logFormat = new VisageFormatter();
	public static final Logger log = Logger.getLogger("com.surgeplay.visage");
	
	public static boolean debug, trace;
	public static boolean ansi;
	public static VisageRunner runner;
	
	public static void main(String[] args) throws Exception {
		AnsiConsole.systemInstall();
		Thread.currentThread().setName("Main thread");
		ConsoleHandler con = new ConsoleHandler();
		con.setFormatter(logFormat);
		log.setUseParentHandlers(false);
		log.addHandler(con);
		if (Boolean.parseBoolean(System.getProperty("com.surgeplay.visage.trace"))
				|| Boolean.parseBoolean(System.getProperty("com.gameminers.visage.trace"))) { // gm option kept for compatibility
			trace = debug = true;
			log.setLevel(Level.ALL);
			con.setLevel(Level.ALL);
		} else if (Boolean.parseBoolean(System.getProperty("com.surgeplay.visage.debug"))
				|| Boolean.parseBoolean(System.getProperty("com.gameminers.visage.debug"))) {
			debug = true;
			log.setLevel(Level.FINER);
			con.setLevel(Level.FINER);
		} else {
			log.setLevel(Level.FINE);
			con.setLevel(Level.FINE);
		}
		OptionParser parser = new OptionParser();
		// master/slave terminology kept for compatibility
		OptionSpec<?> help = parser.acceptsAll(Arrays.asList("h", "help", "?"), "Show this help").forHelp();
		parser.acceptsAll(Arrays.asList("distributor", "d", "master", "m"), "Start Visage as a distributor");
		parser.acceptsAll(Arrays.asList("renderer", "r", "slave", "s"), "Start Visage as a renderer");
		parser.accepts("pollute-my-directory", "Bypass directory checks when downloading defaults");
		OptionSpec<File> fileSwitch;
		fileSwitch = parser.acceptsAll(Arrays.asList("config", "c"), "Load the given config file instead of the default conf/[mode].conf, and don't download defaults").withRequiredArg().ofType(File.class);
		OptionSet set = parser.parse(args);

		if (set.has(help)) {
			System.err.println("Visage v"+VERSION);
			parser.printHelpOn(System.err);
			System.exit(0);
		}
		
		if (!set.has(fileSwitch)) {
			File conf = new File("conf");
			if (!conf.isDirectory()) {
				ansi = true;
				log.info("Looks like you're starting Visage for the first time.");
				log.info("We'll download some defaults to get you started.");
				File wd = new File(".");
				if (wd.list().length > 1) {
					if (set.has("pollute-my-directory")) {
						log.warning("This directory doesn't seem to be empty, but continuing anyway, as you requested.");
					} else {
						log.severe("This directory doesn't seem to be empty.");
						log.severe("To prevent a frustrating experience akin to a zip bomb, Visage will exit.");
						log.severe("If you want to download the defaults into this directory anyway, run Visage again with the option --pollute-my-directory.");
						log.severe("Alternatively, specify a config file with --config and Visage will not attempt to download defaults.");
						System.exit(4);
						return;
					}
				} else {
					log.fine("Directory seems to be empty.");
				}
				log.info("Creating conf directory");
				conf.mkdirs();
				if (!conf.isDirectory()) throw new FileNotFoundException("Failed to create conf directory");
				File www = new File("www");
				log.info("Creating www directory");
				www.mkdirs();
				if (!www.isDirectory()) throw new FileNotFoundException("Failed to create www directory");
				
				String builtFrom = System.getProperty("com.surgeplay.visage.internal.builtFrom", "master");
				log.info("Pulling files from ref "+builtFrom);
				
				download("conf/renderer.conf", builtFrom);
				download("conf/distributor.conf", builtFrom);
				
				download("www/index.html", builtFrom);
			}
		}
		
		File confFile = fileSwitch.value(set);
		if (set.has("distributor")) {
			if (confFile == null) {
				confFile = new File("conf/distributor.conf");
				if (!confFile.exists()) {
					confFile = new File("conf/master.conf");
				}
			}
			Config conf = ConfigFactory.parseFile(confFile);
			ansi = conf.getBoolean("ansi");
			log.info("Starting Visage v"+VERSION+" as a distributor");
			runner = new VisageDistributor(conf);
		} else {
			if (confFile == null) {
				confFile = new File("conf/renderer.conf");
				if (!confFile.exists()) {
					confFile = new File("conf/slave.conf");
				}
			}
			Config conf = ConfigFactory.parseFile(confFile);
			ansi = conf.getBoolean("ansi");
			log.info("Starting Visage v"+VERSION+" as a renderer");
			runner = new VisageRenderer(conf);
		}
		if (trace) {
			log.warning("You have trace logging enabled. This will impact performance.");
		}
		log.info("Reading configuration from "+confFile);
		log.info("Press Ctrl+C to shutdown Visage.");
		runner.start();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				runner.shutdown();
			}
		});
	}

	private static void download(String path, String commit) throws MalformedURLException, IOException {
		log.info("Downloading "+path);
		Resources.asByteSource(new URL("https://raw.githubusercontent.com/surgeplay/Visage/"+commit+"/"+path))
			.copyTo(Files.asByteSink(new File(path)));
	}
}
