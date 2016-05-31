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

import java.util.logging.Level;

import com.surgeplay.visage.slave.render.BustRenderer;
import com.surgeplay.visage.slave.render.FaceRenderer;
import com.surgeplay.visage.slave.render.FrontFullRenderer;
import com.surgeplay.visage.slave.render.FrontRenderer;
import com.surgeplay.visage.slave.render.FullRenderer;
import com.surgeplay.visage.slave.render.HeadRenderer;
import com.surgeplay.visage.slave.render.Renderer;

public enum RenderMode {
	FACE(FaceRenderer.class),
	HEAD(HeadRenderer.class),
	BUST(BustRenderer.class),
	FULL(FullRenderer.class),
	SKIN(null),
	FRONT(FrontRenderer.class),
	FRONTFULL(FrontFullRenderer.class),
	;
	private final Class<? extends Renderer> renderer;
	private RenderMode(Class<? extends Renderer> renderer) {
		this.renderer = renderer;
	}
	public Renderer newRenderer() {
		if (renderer == null) return null;
		try {
			return renderer.newInstance();
		} catch (Exception e) {
			Visage.log.log(Level.SEVERE, "Could not instanciate Renderer for "+this, e);
			return null;
		}
	}
}
