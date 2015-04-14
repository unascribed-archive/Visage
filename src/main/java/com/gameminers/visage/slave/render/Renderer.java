package com.gameminers.visage.slave.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;

import com.gameminers.visage.Visage;
import com.gameminers.visage.slave.render.primitive.Primitive;
import com.google.common.collect.Lists;

import static org.lwjgl.opengl.ARBVertexBufferObject.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.Util.checkGLError;
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
		Visage.log.finer("Uploading "+width+"x"+height+" ("+(width*height)+" pixel) image");
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
		Visage.log.finer("["+name+"] Initializing Pbuffer (assuming "+supersampling+"x supersampling)");
		if (pbuffer != null) {
			destroy();
		}
		pbuffer = new Pbuffer(512*supersampling, 512*supersampling, new PixelFormat(8, 8, 0), null, null, new ContextAttribs(1, 2));
		if (pbuffer.isBufferLost())
			throw new LWJGLException("Could not create Pbuffer");
		pbuffer.makeCurrent();
		
		Visage.log.finer("["+name+"] Setting up VBOs");
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
		lightColor.put(1f);
		lightColor.put(0.9f);
		lightColor.put(0.6f);
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
		
		Visage.log.finer("["+name+"] Initializing primitives");
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

		prims.clear();
		initPrimitives(); // TODO remove, this method should only be called during first init
	}
	public void finish() throws LWJGLException {
		pbuffer.releaseContext();
	}
}
