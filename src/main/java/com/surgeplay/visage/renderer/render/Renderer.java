/*
 * The MIT License
 *
 * Copyright (c) 2015-2018, Una Thompson (unascribed)
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

package com.surgeplay.visage.renderer.render;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.List;

import org.lwjgl.BufferUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.surgeplay.visage.Visage;
import com.surgeplay.visage.renderer.RenderContext;
import com.surgeplay.visage.renderer.render.primitive.Primitive;
import static com.surgeplay.visage.renderer.util.Errors.checkGLError;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

public abstract class Renderer {
	public final String name = getClass().getSimpleName();
	public List<Primitive> prims = Lists.newArrayList();
	
	private boolean initialized = false;
	
	public RenderContext owner;
	
	public Renderer(RenderContext owner) {
		this.owner = owner;
	}
	
	protected void addPrimitive(Primitive prim) {
		prims.add(prim);
	}
	
	protected void preRender(int width, int height) {}
	protected void postRender(int width, int height) {}
	
	public void render(int width, int height) {
		initGL(width, height);
		preRender(width, height);
		for (Primitive prim : prims) {
			prim.render(this);
		}
		postRender(width, height);
		checkGLError();
	}
	
	public void destroy() {
		prims.clear();
		initialized = false;
	}
	
	public void init(boolean slim, boolean full, boolean flip) {
		if (Visage.debug) {
			List<String> modes = Lists.newArrayList();
			if (slim) modes.add("slim");
			if (full) modes.add("full");
			if (flip) modes.add("flip");
			Visage.log.finer("["+name+"] Initializing primitives"+(modes.isEmpty() ? "" : " ("+Joiner.on(", ").join(modes)+")"));
		}
		initPrimitives(slim, full, flip);
		initialized = true;
	}
	
	public boolean isInitialized() {
		return initialized;
	}
	
	protected abstract void initPrimitives(boolean slim, boolean full, boolean flip);
	protected void initGL(float width, float height) {
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glViewport(0, 0, (int)width, (int)height);
		glEnable(GL_DEPTH_TEST);
		
		double fov = 45;
		double aspect = width/height;
		
		double zNear = 0.1;
		double zFar = 100;
		
		double fH = Math.tan((fov / 360) * Math.PI) * zNear;
		double fW = fH * aspect;
		glFrustum(-fW, fW, -fH, fH, zNear, zFar);
		
		glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glEnable(GL_CULL_FACE);
	}
	
	public void finish() {
	}
	
	public BufferedImage readPixels(int width, int height) {
		glReadBuffer(GL_FRONT);
		ByteBuffer buf = BufferUtils.createByteBuffer(width * height * 4);
		glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_BYTE, buf);
		checkGLError();
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int[] pixels = new int[width*height];
		buf.asIntBuffer().get(pixels);
		img.setRGB(0, 0, width, height, pixels, 0, width);
		if (Visage.trace) Visage.log.finest("Read pixels");
		return img;
	}
}
