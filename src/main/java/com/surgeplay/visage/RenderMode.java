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
package com.surgeplay.visage;

import java.util.logging.Level;

import com.surgeplay.visage.renderer.RenderContext;
import com.surgeplay.visage.renderer.render.BustRenderer;
import com.surgeplay.visage.renderer.render.BustSlimRenderer;
import com.surgeplay.visage.renderer.render.FaceRenderer;
import com.surgeplay.visage.renderer.render.FrontFullRenderer;
import com.surgeplay.visage.renderer.render.FrontRenderer;
import com.surgeplay.visage.renderer.render.FrontFullSlimRenderer;
import com.surgeplay.visage.renderer.render.FrontSlimRenderer;
import com.surgeplay.visage.renderer.render.FullRenderer;
import com.surgeplay.visage.renderer.render.FullSlimRenderer;
import com.surgeplay.visage.renderer.render.HeadRenderer;
import com.surgeplay.visage.renderer.render.Renderer;

public enum RenderMode {
	FACE(FaceRenderer.class),
	HEAD(HeadRenderer.class),
	
	BUST(BustRenderer.class),
	BUST_SLIM(BustSlimRenderer.class),
	
	FULL(FullRenderer.class),
	FULL_SLIM(FullSlimRenderer.class),
	
	FRONT(FrontRenderer.class),
	FRONT_SLIM(FrontSlimRenderer.class),
	
	FRONTFULL(FrontFullRenderer.class),
	FRONTFULL_SLIM(FrontFullSlimRenderer.class),
	
	SKIN(null),
	;
	
	private final Class<? extends Renderer> renderer;
	
	private RenderMode(Class<? extends Renderer> renderer) {
		this.renderer = renderer;
	}
	
	public boolean isTall() {
		switch (this) {
			case FULL: return true;
			case FULL_SLIM: return true;
			case FRONTFULL: return true;
			case FRONTFULL_SLIM: return true;
			default: return false;
		}
	}
	
	public RenderMode slim() {
		switch (this) {
			case BUST: return BUST_SLIM;
			case FULL: return FULL_SLIM;
			case FRONT: return FRONT_SLIM;
			case FRONTFULL: return FRONTFULL_SLIM;
			default: return this;
		}
	}
	
	public Renderer newRenderer(RenderContext ctx) {
		if (renderer == null) return null;
		try {
			return renderer.getConstructor(RenderContext.class).newInstance(ctx);
		} catch (Exception e) {
			Visage.log.log(Level.SEVERE, "Could not instanciate Renderer for "+this, e);
			return null;
		}
	}
}
