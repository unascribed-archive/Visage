package com.gameminers.visage.master.glue;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.eclipse.jetty.util.log.AbstractLogger;
import org.eclipse.jetty.util.log.Logger;

public class LogShim extends AbstractLogger {
	private boolean debug = false;
	private final java.util.logging.Logger log;
	public LogShim(java.util.logging.Logger log) {
		this.log = log;
	}

	@Override
	public String getName() {
		return "LogShim";
	}

	@Override
	public void warn(String msg, Object... args) {
		log.warning(String.format(msg, args));
	}

	@Override
	public void warn(Throwable thrown) {
		LogRecord rec = new LogRecord(Level.WARNING, "");
		rec.setThrown(thrown);
		log.log(rec);
	}

	@Override
	public void warn(String msg, Throwable thrown) {
		LogRecord rec = new LogRecord(Level.WARNING, msg);
		rec.setThrown(thrown);
		log.log(rec);
	}

	@Override
	public void info(String msg, Object... args) {
		log.info(String.format(msg, args));
	}

	@Override
	public void info(Throwable thrown) {
		LogRecord rec = new LogRecord(Level.INFO, "");
		rec.setThrown(thrown);
		log.log(rec);
	}

	@Override
	public void info(String msg, Throwable thrown) {
		LogRecord rec = new LogRecord(Level.INFO, msg);
		rec.setThrown(thrown);
		log.log(rec);
	}

	@Override
	public boolean isDebugEnabled() {
		return debug;
	}

	@Override
	public void setDebugEnabled(boolean enabled) {
		debug = enabled;
	}

	@Override
	public void debug(String msg, Object... args) {
		log.finer(String.format(msg, args));
	}

	@Override
	public void debug(String msg, long value) {
		log.finer(String.format(msg, value));
	}

	@Override
	public void debug(Throwable thrown) {
		LogRecord rec = new LogRecord(Level.FINER, "");
		rec.setThrown(thrown);
		log.log(rec);
	}

	@Override
	public void debug(String msg, Throwable thrown) {
		LogRecord rec = new LogRecord(Level.FINER, msg);
		rec.setThrown(thrown);
		log.log(rec);
	}
	
	@Override
	public void ignore(Throwable ignored) {}

	@Override
	protected Logger newLogger(String fullname) {
		return new LogShim(log);
	}

}
