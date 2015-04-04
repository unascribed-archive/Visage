/* This file is part of MCAuthLib.
 * Copyright (C) 2013-2014 Steveice10
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.spacehq.mc.auth.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

public class IOUtils {

	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

	public static void closeQuietly(Closeable close) {
		try {
			if(close != null) {
				close.close();
			}
		} catch(IOException e) {
		}
	}

	public static String toString(InputStream input, String encoding) throws IOException {
		StringWriter writer = new StringWriter();
		InputStreamReader in = encoding != null ? new InputStreamReader(input, encoding) : new InputStreamReader(input);
		char[] buffer = new char[DEFAULT_BUFFER_SIZE];
		int n = 0;
		while(-1 != (n = in.read(buffer))) {
			writer.write(buffer, 0, n);
		}

		in.close();
		return writer.toString();
	}

	public static byte[] toByteArray(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte buffer[] = new byte[DEFAULT_BUFFER_SIZE];
		int n = 0;
		while(-1 != (n = in.read(buffer))) {
			out.write(buffer, 0, n);
		}

		in.close();
		out.close();
		return out.toByteArray();
	}

}
