/*
 * The MIT License
 *
 * Copyright (c) 2015-2017, William Thompson (unascribed)
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

package com.surgeplay.visage.renderer;

import com.surgeplay.visage.RenderMode;
import com.surgeplay.visage.renderer.render.BodyRenderer;
import com.surgeplay.visage.renderer.render.FaceRenderer;
import com.surgeplay.visage.renderer.render.FlatBodyRenderer;
import com.surgeplay.visage.renderer.render.HeadRenderer;
import com.surgeplay.visage.renderer.render.Renderer;

public final class RenderConfiguration {

	public enum Type {
		FACE,
		FLAT_BODY,
		BODY,
		HEAD;

		public static Type fromMode(RenderMode mode) {
			switch (mode) {
				case FACE:
					return FACE;
				case FRONT:
				case FRONTFULL:
					return FLAT_BODY;
				case BUST:
				case FULL:
					return BODY;
				case HEAD:
					return HEAD;
				default:
					throw new AssertionError("No mapping for "+mode);
			}
		}
	}
	
	private Type type;
	
	private boolean slim;
	private boolean full;
	private boolean flip;
	
	private boolean locked;
	
	public RenderConfiguration(Type type, boolean slim, boolean full, boolean flip) {
		if (type == null) {
			throw new IllegalArgumentException("type cannot be null");
		}
		this.type = type;
		this.slim = slim;
		this.full = full;
		this.flip = flip;
	}
	
	public Renderer createRenderer(RenderContext owner) {
		Renderer r;
		switch (type) {
			case FACE:
				r = new FaceRenderer(owner);
				break;
			case FLAT_BODY:
				r = new FlatBodyRenderer(owner);
				break;
			case BODY:
				r = new BodyRenderer(owner);
				break;
			case HEAD:
				r = new HeadRenderer(owner);
				break;
			default:
				throw new AssertionError("Missing mapping for "+type);
		}
		r.init(slim, full, flip);
		return r;
	}
	
	public RenderConfiguration lock() {
		locked = true;
		return this;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		if (locked) throw new IllegalStateException("Cannot modify locked RenderConfiguration");
		if (type == null) {
			throw new IllegalArgumentException("type cannot be null");
		}
		this.type = type;
	}

	public boolean isSlim() {
		if (type == Type.HEAD || type == Type.FACE) {
			return false;
		}
		return slim;
	}

	public void setSlim(boolean slim) {
		if (locked) throw new IllegalStateException("Cannot modify locked RenderConfiguration");
		this.slim = slim;
	}

	public boolean isFull() {
		if (type == Type.HEAD || type == Type.FACE) {
			return false;
		}
		return full;
	}

	public void setFull(boolean full) {
		if (locked) throw new IllegalStateException("Cannot modify locked RenderConfiguration");
		this.full = full;
	}

	public boolean isFlipped() {
		return flip;
	}

	public void setFlipped(boolean flip) {
		if (locked) throw new IllegalStateException("Cannot modify locked RenderConfiguration");
		this.flip = flip;
	}

	public boolean isLocked() {
		return locked;
	}
	
	public RenderConfiguration copy() {
		return new RenderConfiguration(type, slim, full, flip);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isFlipped() ? 1231 : 1237);
		result = prime * result + (isFull() ? 1231 : 1237);
		result = prime * result + (isSlim() ? 1231 : 1237);
		result = prime * result + (getType().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		RenderConfiguration other = (RenderConfiguration) obj;
		if (isFlipped() != other.isFlipped())
			return false;
		if (isFull() != other.isFull())
			return false;
		if (isSlim() != other.isSlim())
			return false;
		if (getType() != other.getType())
			return false;
		return true;
	}

	
	
}
