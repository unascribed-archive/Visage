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
package com.surgeplay.visage.slave.render;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;

import com.google.common.collect.Lists;
import com.surgeplay.visage.Visage;
import com.surgeplay.visage.slave.render.primitive.Primitive;
import com.surgeplay.visage.util.Images;

import static com.surgeplay.visage.slave.util.Errors.checkGLError;

import static org.lwjgl.opengl.ARBVertexBufferObject.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.util.glu.GLU.gluPerspective;

public abstract class Renderer {
	public static final float normalX = -0.2f;
	public static final float normalY = 0;
	public static final float normalZ = -1;
	public static final float[] vertices = {
		// Front
		-1.0f, -1.0f,  1.0f,
		normalX, normalY, normalZ,
		 1.0f, -1.0f,  1.0f,
		normalX, normalY, normalZ,
		 1.0f,  1.0f,  1.0f,
		normalX, normalY, normalZ,
		-1.0f,  1.0f,  1.0f,
		normalX, normalY, normalZ,
		// Back
		-1.0f, -1.0f, -1.0f,
		normalX, normalY, normalZ,
		 1.0f, -1.0f, -1.0f,
		normalX, normalY, normalZ,
		 1.0f,  1.0f, -1.0f,
		normalX, normalY, normalZ,
		-1.0f,  1.0f, -1.0f,
		normalX, normalY, normalZ,
		// Top
		-1.0f,  1.0f,  1.0f,
		normalX, normalY, normalZ,
		 1.0f,  1.0f,  1.0f,
		normalX, normalY, normalZ,
		 1.0f,  1.0f, -1.0f,
		normalX, normalY, normalZ,
		-1.0f,  1.0f, -1.0f,
		normalX, normalY, normalZ,
		// Bottom
		-1.0f, -1.0f, -1.0f,
		normalX, normalY, normalZ,
		 1.0f, -1.0f, -1.0f,
		normalX, normalY, normalZ,
		 1.0f, -1.0f,  1.0f,
		normalX, normalY, normalZ,
		-1.0f, -1.0f,  1.0f,
		normalX, normalY, normalZ,
		// Left
		 1.0f, -1.0f,  1.0f,
		normalX, normalY, normalZ,
		 1.0f, -1.0f, -1.0f,
		normalX, normalY, normalZ,
		 1.0f,  1.0f, -1.0f,
		normalX, normalY, normalZ,
		 1.0f,  1.0f,  1.0f,
		normalX, normalY, normalZ,
		// Right
		-1.0f, -1.0f, -1.0f,
		normalX, normalY, normalZ,
		-1.0f, -1.0f,  1.0f,
		normalX, normalY, normalZ,
		-1.0f,  1.0f,  1.0f,
		normalX, normalY, normalZ,
		-1.0f,  1.0f, -1.0f,
		normalX, normalY, normalZ,
	};
	public static final float[] planeVertices = {
		-1.0f,  0.0f,  1.0f,
		 0.000f, 0.000f,
		 1.0f,  0.0f,  1.0f,
		 1.000f, 0.000f,
		 1.0f,  0.0f, -1.0f,
		 1.000f, 1.000f,
		-1.0f,  0.0f, -1.0f,
		 0.000f, 1.000f,
	};
	public final String name = getClass().getSimpleName();
	public Pbuffer pbuffer;
	public List<Primitive> prims = Lists.newArrayList();
	public int vbo, planeVbo, texture, shadowTexture;
	
	public FloatBuffer lightColor;
	public FloatBuffer lightPosition;
	
	private int supersampling;
	private boolean initialized = false;
	private static final BufferedImage shadow;
	static {
		BufferedImage img = null;
		try {
			img = ImageIO.read(ClassLoader.getSystemResource("shadow.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		shadow = img;
	}
	protected void addPrimitive(Primitive prim) {
		prims.add(prim);
	}
	public void setSkin(BufferedImage img) throws LWJGLException {
		pbuffer.makeCurrent();
		upload(img, texture);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		checkGLError();
	}
	public void upload(BufferedImage img, int tex) {
		int width = img.getWidth();
		int height = img.getHeight();
		if (Visage.debug) Visage.log.finer("Uploading "+width+"x"+height+" ("+(width*height)+" pixel) image");
		int[] argb = new int[width*height];
		img.getRGB(0, 0, width, height, argb, 0, width);
		IntBuffer buf = BufferUtils.createIntBuffer(width*height);
		buf.put(argb);
		buf.flip();
		glBindTexture(GL_TEXTURE_2D, tex);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_BGRA, GL_UNSIGNED_BYTE, buf);
		checkGLError();
	}
	protected void preRender(int width, int height) throws LWJGLException {}
	protected void postRender(int width, int height) throws LWJGLException {}
	public void render(int width, int height) throws LWJGLException {
		initGL(width, height);
		preRender(width, height);
		for (Primitive prim : prims) {
			prim.render(this);
		}
		postRender(width, height);
		checkGLError();
	}
	public void destroy() {
		if (pbuffer != null) {
			pbuffer.destroy();
			pbuffer = null;
		}
		vbo = planeVbo = texture = shadowTexture = 0;
		prims.clear();
		initialized = false;
	}
	public void init(int supersampling) throws LWJGLException {
		this.supersampling = supersampling;
		if (Visage.debug) Visage.log.finer("["+name+"] Initializing Pbuffer (assuming "+supersampling+"x supersampling)");
		if (pbuffer != null) {
			destroy();
		}
		int width = 512;
		int height = 512;
		if (this instanceof FullRenderer) {
			height = 832;
		}
		pbuffer = new Pbuffer(width*supersampling, height*supersampling, new PixelFormat(8, 8, 0), null, null, new ContextAttribs(1, 2));
		if (pbuffer.isBufferLost())
			throw new LWJGLException("Could not create Pbuffer");
		pbuffer.makeCurrent();
		
		if (Visage.debug) Visage.log.finer("["+name+"] Setting up VBOs");
		IntBuffer ids = BufferUtils.createIntBuffer(2);
		glGenBuffersARB(ids);
		vbo = ids.get();
		planeVbo = ids.get();
		checkGLError();
		
		IntBuffer textures = BufferUtils.createIntBuffer(2);
		glGenTextures(textures);
		texture = textures.get();
		shadowTexture = textures.get();
		checkGLError();
		
		upload(shadow, shadowTexture);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		
		FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
		vertexBuffer.put(vertices);
		vertexBuffer.flip();
		glBindBufferARB(GL_ARRAY_BUFFER_ARB, vbo);
		glBufferDataARB(GL_ARRAY_BUFFER_ARB, vertexBuffer, GL_STATIC_DRAW_ARB);
		checkGLError();
		
		FloatBuffer planeVertexBuffer = BufferUtils.createFloatBuffer(planeVertices.length);
		planeVertexBuffer.put(planeVertices);
		planeVertexBuffer.flip();
		glBindBufferARB(GL_ARRAY_BUFFER_ARB, planeVbo);
		glBufferDataARB(GL_ARRAY_BUFFER_ARB, planeVertexBuffer, GL_STATIC_DRAW_ARB);
		checkGLError();
		
		glClearColor(0, 0, 0, 0);
		glClearDepth(1.0);
		checkGLError();
		
		glShadeModel(GL_SMOOTH);
		glCullFace(GL_FRONT);
		glFrontFace(GL_CCW);
		checkGLError();
		
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LEQUAL);
		checkGLError();
		
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		checkGLError();
		
		lightColor = BufferUtils.createFloatBuffer(4);
		lightColor.put(3f);
		lightColor.put(3f);
		lightColor.put(3f);
		lightColor.put(1.0f);
		lightColor.flip();
		glLight(GL_LIGHT0, GL_AMBIENT, lightColor);
		
		lightPosition = BufferUtils.createFloatBuffer(4);
		lightPosition.put(-4f);
		lightPosition.put(-2f);
		lightPosition.put(1f);
		lightPosition.put(1000f);
		lightPosition.flip();

		glEnable(GL_LIGHTING);
		glEnable(GL_LIGHT0);
		checkGLError();
		
		if (Visage.debug) Visage.log.finer("["+name+"] Initializing primitives");
		initPrimitives();
		initialized = true;
	}
	public boolean isInitialized() {
		return initialized;
	}
	protected abstract void initPrimitives();
	protected void initGL(float width, float height) throws LWJGLException {
		if (pbuffer.isBufferLost()) {
			Visage.log.warning("We appear to have lost the Pbuffer. Checking under the couch cushions...");
			Visage.log.info("Nope. Can't find it. Creating a new Pbuffer...");
			destroy();
			init(supersampling);
		}
		pbuffer.makeCurrent();
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glViewport(0, 0, (int)width, (int)height);
		gluPerspective(
				45.0f,
				width / height,
				0.1f,
				100.0f);
		glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
		glMatrixMode(GL_MODELVIEW);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glEnable(GL_CULL_FACE);
		
	}
	public void finish() throws LWJGLException {
		pbuffer.releaseContext();
	}
	public BufferedImage readPixels(int width, int height) throws InterruptedException {
		glReadBuffer(GL_FRONT);
		ByteBuffer buf = BufferUtils.createByteBuffer(width * height * 4);
		glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_BYTE, buf);
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int[] pixels = new int[width*height];
		buf.asIntBuffer().get(pixels);
		img.setRGB(0, 0, width, height, pixels, 0, width);
		if (Visage.trace) Visage.log.finest("Read pixels");
		return Images.toBuffered(img.getScaledInstance(width/supersampling, height/supersampling, Image.SCALE_AREA_AVERAGING));
	}
}
