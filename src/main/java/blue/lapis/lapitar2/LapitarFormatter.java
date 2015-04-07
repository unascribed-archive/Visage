package blue.lapis.lapitar2;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

public class LapitarFormatter extends Formatter {
	private final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
	private final Map<Level, Color> colors = Maps.newHashMap();
	private final Map<Level, String> names = Maps.newHashMap();
	public LapitarFormatter() {
		colors.put(Level.FINER, Color.BLACK);
		colors.put(Level.FINE, Color.GREEN);
		colors.put(Level.INFO, Color.BLUE);
		colors.put(Level.WARNING, Color.YELLOW);
		colors.put(Level.SEVERE, Color.RED);
		colors.put(Level.OFF, Color.MAGENTA);
		
		names.put(Level.FINEST, "TRACE");
		names.put(Level.FINER, "DEBUG");
		names.put(Level.FINE, " FINE");
		names.put(Level.INFO, " INFO");
		names.put(Level.WARNING, " WARN");
		names.put(Level.SEVERE, "ERROR");
		names.put(Level.OFF, "FATAL");
	}
	@Override
	public String format(LogRecord record) {
		if (record.getThrown() != null) {
			record.getThrown().printStackTrace();
		}
		Ansi ansi = Ansi.ansi();
		if (Lapitar.ansi) {
			ansi.fgBright(Color.BLACK);
		}
		Date date = new Date(record.getMillis());
		ansi.a("@");
		ansi.a(format.format(date));
		if (Lapitar.ansi) {
			ansi.reset();
		}
		ansi.a(Strings.padStart(Thread.currentThread().getName(), 22, ' '));
		ansi.a(" ");
		if (Lapitar.ansi && colors.containsKey(record.getLevel())) {
			ansi.fgBright(colors.get(record.getLevel()));
		}
		ansi.a(names.get(record.getLevel()));
		if (Lapitar.ansi) {
			ansi.reset();
		}
		ansi.a(": ");
		if (Lapitar.ansi && colors.containsKey(record.getLevel()) && record.getLevel().intValue() >= Level.SEVERE.intValue()) {
			ansi.bold();
			ansi.fgBright(colors.get(record.getLevel()));
		}
		ansi.a(record.getMessage());
		if (Lapitar.ansi) {
			ansi.reset();
		}
		ansi.a("\n");
		return ansi.toString();
	}

}
