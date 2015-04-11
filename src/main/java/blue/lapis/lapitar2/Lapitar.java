package blue.lapis.lapitar2;

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
import blue.lapis.lapitar2.benchmark.LapitarBenchmark;
import blue.lapis.lapitar2.master.LapitarMaster;
import blue.lapis.lapitar2.slave.LapitarSlave;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Lapitar {
	public static final String VERSION = "2.0.0";
	public static final Formatter logFormat = new LapitarFormatter();
	public static final Logger log = Logger.getLogger("blue.lapis.lapitar2");
	public static boolean ansi;
	public static void main(String[] args) throws Exception {
		AnsiConsole.systemInstall();
		Thread.currentThread().setName("Main thread");
		ConsoleHandler con = new ConsoleHandler();
		con.setFormatter(logFormat);
		log.setUseParentHandlers(false);
		log.addHandler(con);
		if (Boolean.parseBoolean(System.getProperty("blue.lapis.lapitar2.trace"))) {
			log.setLevel(Level.ALL);
			con.setLevel(Level.ALL);
		} else if (Boolean.parseBoolean(System.getProperty("blue.lapis.lapitar2.debug"))) {
			log.setLevel(Level.FINER);
			con.setLevel(Level.FINER);
		} else {
			log.setLevel(Level.FINE);
			con.setLevel(Level.FINE);
		}
		OptionParser parser = new OptionParser();
		parser.acceptsAll(Arrays.asList("master", "m"), "Start Lapitar as a master");
		parser.acceptsAll(Arrays.asList("slave", "s"), "Start Lapitar as a slave");
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
			log.info("Starting Lapitar v"+VERSION+" as a master");
			new LapitarMaster(conf).start();
		} else if (set.has("slave")) {
			if (confFile == null) {
				confFile = new File("conf/slave.conf");
			}
			Config conf = ConfigFactory.parseFile(confFile);
			ansi = conf.getBoolean("ansi");
			log.info("Starting Lapitar v"+VERSION+" as a slave");
			new LapitarSlave(conf).start();
		} else if (set.has("benchmark")) {
			ansi = true;
			log.info("Running a benchmark...");
			new LapitarBenchmark().start();
		} else {
			System.err.println("You must specify a mode to start Lapitar in.");
			parser.printHelpOn(System.err);
		}
	}
}
