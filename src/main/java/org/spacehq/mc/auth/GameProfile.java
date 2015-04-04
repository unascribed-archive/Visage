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
package org.spacehq.mc.auth;

import java.util.UUID;

import org.spacehq.mc.auth.properties.PropertyMap;

public class GameProfile {

	private UUID id;
	private String name;
	private PropertyMap properties = new PropertyMap();
	private boolean legacy;

	public GameProfile(String id, String name) {
		this(id == null || id.equals("") ? null : UUID.fromString(id), name);
	}

	public GameProfile(UUID id, String name) {
		if(id == null && (name == null || name.equals("")))
			throw new IllegalArgumentException("Name and ID cannot both be blank");
		else {
			this.id = id;
			this.name = name;
		}
	}

	public UUID getId() {
		return this.id;
	}

	public String getIdAsString() {
		return this.id != null ? this.id.toString() : "";
	}

	public String getName() {
		return this.name;
	}

	public PropertyMap getProperties() {
		return this.properties;
	}

	public boolean isLegacy() {
		return this.legacy;
	}

	public boolean isComplete() {
		return this.id != null && this.name != null && !this.name.equals("");
	}

	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		else if(o != null && this.getClass() == o.getClass()) {
			GameProfile that = (GameProfile) o;
			return (this.id != null ? this.id.equals(that.id) : that.id == null) && (this.name != null ? this.name.equals(that.name) : that.name == null);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		int result = this.id != null ? this.id.hashCode() : 0;
		result = 31 * result + (this.name != null ? this.name.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "GameProfile{id=" + this.id + ", name=" + this.name + ", properties=" + this.properties + ", legacy=" + this.legacy + "}";
	}

}
