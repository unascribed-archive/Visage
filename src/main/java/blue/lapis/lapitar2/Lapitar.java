package blue.lapis.lapitar2;

import java.io.File;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import blue.lapis.lapitar2.benchmark.LapitarBenchmark;
import blue.lapis.lapitar2.master.LapitarMaster;
import blue.lapis.lapitar2.slave.LapitarSlave;

import com.typesafe.config.ConfigFactory;

public class Lapitar {
	public static final String VERSION = "2.0.0";
	public static final Formatter logFormat = new LapitarFormatter();
	public static final Logger log = Logger.getLogger("blue.lapis.lapitar2");
	public static void main(String[] args) throws Exception {
		Thread.currentThread().setName("Main thread");
		ConsoleHandler con = new ConsoleHandler();
		con.setFormatter(logFormat);
		log.setUseParentHandlers(false);
		log.addHandler(con);
		OptionParser parser = new OptionParser();
		parser.acceptsAll(Arrays.asList("master", "m"), "Start Lapitar as a master.");
		parser.acceptsAll(Arrays.asList("slave", "s"), "Start Lapitar as a slave.");
		parser.acceptsAll(Arrays.asList("benchmark", "b"), "Run a benchmark on the current machine.");
		OptionSet set = parser.parse(args);
		if (set.has("master")) {
			log.info("Starting Lapitar v"+VERSION+" as a master");
			new LapitarMaster(ConfigFactory.parseFile(new File("conf/master.conf"))).start();
		} else if (set.has("slave")) {
			log.info("Starting Lapitar v"+VERSION+" as a slave");
			new LapitarSlave(ConfigFactory.parseFile(new File("conf/slave.conf"))).start();
		} else if (set.has("benchmark")) {
			log.info("Running a benchmark...");
			new LapitarBenchmark().start();
		} else {
			System.err.println("You must specify a mode to start Lapitar in.");
			parser.printHelpOn(System.err);
		}
	}
}
