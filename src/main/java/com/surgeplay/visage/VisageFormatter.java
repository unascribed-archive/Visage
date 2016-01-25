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

public class VisageFormatter extends Formatter {
	private final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
	private final Map<Level, Color> colors = Maps.newHashMap();
	private final Map<Level, String> names = Maps.newHashMap();
	public VisageFormatter() {
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
		if (Visage.ansi) {
			ansi.fgBright(Color.BLACK);
		}
		Date date = new Date(record.getMillis());
		ansi.a("@");
		ansi.a(format.format(date));
		if (Visage.ansi) {
			ansi.reset();
		}
		ansi.a(Strings.padStart(Thread.currentThread().getName(), 22, ' '));
		ansi.a(" ");
		if (Visage.ansi && colors.containsKey(record.getLevel())) {
			ansi.fgBright(colors.get(record.getLevel()));
		}
		ansi.a(names.get(record.getLevel()));
		if (Visage.ansi) {
			ansi.reset();
		}
		ansi.a(": ");
		if (Visage.ansi && colors.containsKey(record.getLevel()) && record.getLevel().intValue() >= Level.SEVERE.intValue()) {
			ansi.bold();
			ansi.fgBright(colors.get(record.getLevel()));
		}
		ansi.a(record.getMessage());
		if (Visage.ansi) {
			ansi.reset();
		}
		ansi.a("\n");
		return ansi.toString();
	}

}
