package blue.lapis.lapitar2;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LapitarFormatter extends Formatter {
	private final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
	@Override
	public String format(LogRecord record) {
		if (record.getThrown() != null) {
			record.getThrown().printStackTrace();
		}
		return "[" + format.format(new Date(record.getMillis())) + "]" + " [" + Thread.currentThread().getName() + "/" + record.getLevel() + "] " + record.getMessage()+"\n";
	}

}
