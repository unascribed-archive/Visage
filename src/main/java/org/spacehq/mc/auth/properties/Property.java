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
package org.spacehq.mc.auth.properties;

import java.security.PublicKey;
import java.security.Signature;

import org.spacehq.mc.auth.exception.SignatureValidateException;
import org.spacehq.mc.auth.util.Base64;

public class Property {

	private String name;
	private String value;
	private String signature;

	public Property(String value, String name) {
		this(value, name, null);
	}

	public Property(String name, String value, String signature) {
		this.name = name;
		this.value = value;
		this.signature = signature;
	}

	public String getName() {
		return this.name;
	}

	public String getValue() {
		return this.value;
	}

	public String getSignature() {
		return this.signature;
	}

	public boolean hasSignature() {
		return this.signature != null;
	}

	public boolean isSignatureValid(PublicKey key) throws SignatureValidateException {
		try {
			Signature sig = Signature.getInstance("SHA1withRSA");
			sig.initVerify(key);
			sig.update(this.value.getBytes());
			return sig.verify(Base64.decode(this.signature.getBytes("UTF-8")));
		} catch(Exception e) {
			throw new SignatureValidateException("Could not validate property signature.", e);
		}
	}

}
