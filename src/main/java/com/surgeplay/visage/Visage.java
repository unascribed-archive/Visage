/*
 * Visage
 * Copyright (c) 2015-2016, Aesen Vismea <aesen@unascribed.com>
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
package com.surgeplay.visage;

import java.io.File;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fusesource.jansi.AnsiConsole;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import com.surgeplay.visage.benchmark.VisageBenchmark;
import com.surgeplay.visage.master.VisageMaster;
import com.surgeplay.visage.slave.VisageSlave;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Visage {
	public static final String VERSION = "1.2.6";
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
		parser.acceptsAll(Arrays.asList("master", "m"), "Start Visage as a master");
		parser.acceptsAll(Arrays.asList("slave", "s"), "Start Visage as a slave");
		parser.acceptsAll(Arrays.asList("benchmark", "b"), "Run a benchmark on the current machine");
		OptionSpec<File> fileSwitch;
		fileSwitch = parser.acceptsAll(Arrays.asList("config", "c"), "Load the given config file instead of the default conf/[mode].conf").withRequiredArg().ofType(File.class);
		OptionSet set = parser.parse(args);
		File confFile = fileSwitch.value(set);
		if (set.has("master")) {
			if (confFile == null) {
				confFile = new File("conf/master.conf");
			}
			Config conf = ConfigFactory.parseFile(confFile);
			ansi = conf.getBoolean("ansi");
			log.info("Starting Visage v"+VERSION+" as a master");
			runner = new VisageMaster(conf);
		} else if (set.has("benchmark")) {
			ansi = true;
			log.info("Running a benchmark...");
			runner = new VisageBenchmark();
		} else {
			if (confFile == null) {
				confFile = new File("conf/slave.conf");
			}
			Config conf = ConfigFactory.parseFile(confFile);
			ansi = conf.getBoolean("ansi");
			log.info("Starting Visage v"+VERSION+" as a slave");
			runner = new VisageSlave(conf);
		}
		if (debug || trace) {
			log.warning("You have debug and/or trace logging enabled. This will severely impact performance.");
		}
		log.info("Press Ctrl+C to shutdown Visage.");
		runner.start();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				runner.shutdown();
			}
		});
	}
}
