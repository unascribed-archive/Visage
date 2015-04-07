package blue.lapis.lapitar2.slave.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.Util;
import org.lwjgl.util.glu.GLU;

import blue.lapis.lapitar2.Lapitar;

import com.google.common.collect.Lists;

import static org.lwjgl.opengl.ARBVertexBufferObject.*;
import static org.lwjgl.opengl.GL11.*;

public abstract class Renderer {
	protected static final float vertices[] = {
		-1.0f, -1.0f,  1.0f,
		 1.0f, -1.0f,  1.0f,
		-1.0f,  1.0f,  1.0f,
		 1.0f,  1.0f,  1.0f,

		 1.0f, -1.0f,  1.0f,
		 1.0f, -1.0f, -1.0f,
		 1.0f,  1.0f,  1.0f,
		 1.0f,  1.0f, -1.0f,

		 1.0f, -1.0f, -1.0f,
		-1.0f, -1.0f, -1.0f,
		 1.0f,  1.0f, -1.0f,
		-1.0f,  1.0f, -1.0f,

		-1.0f, -1.0f, -1.0f,
		-1.0f, -1.0f,  1.0f,
		-1.0f,  1.0f, -1.0f,
		-1.0f,  1.0f,  1.0f,

		-1.0f, -1.0f, -1.0f,
		 1.0f, -1.0f, -1.0f,
		-1.0f, -1.0f,  1.0f,
		 1.0f, -1.0f,  1.0f,

		-1.0f,  1.0f,  1.0f,
		 1.0f,  1.0f,  1.0f,
		-1.0f,  1.0f, -1.0f,
		 1.0f,  1.0f, -1.0f,
	};
	
	protected static final float colors[] = {
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
                               
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
                               
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
                               
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
                               
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
                               
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
	};
	
	protected static final byte indices[] = {
		 0,  1,  3,     0,  3,  2,
		 4,  5,  7,     4,  7,  6,
		 8,  9, 11,     8, 11, 10,
		12, 13, 15,    12, 15, 14,
		16, 17, 19,    16, 19, 18,
		20, 21, 23,    20, 23, 22,
	};
	protected final String name = getClass().getSimpleName();
	protected Pbuffer pbuffer;
	protected List<Cube> cubes = Lists.newArrayList();
	protected int vbo, cvbo, ibo, texture;
	private boolean initialized = false;
	protected void addCube(Cube cube) {
		cubes.add(cube);
	}
	protected void preRender(int width, int height) throws LWJGLException {}
	protected void postRender(int width, int height) throws LWJGLException {}
	public void render(int width, int height) throws LWJGLException {
		initGL(width, height);
		preRender(width, height);
		for (Cube cube : cubes) {
			cube.render(this);
		}
		postRender(width, height);
		Util.checkGLError();
	}
	public void destroy() {
		if (pbuffer != null) {
			pbuffer.destroy();
			pbuffer = null;
		}
		vbo = cvbo = ibo = texture = 0;
		cubes.clear();
		initialized = false;
	}
	public void init(int supersampling) throws LWJGLException {
		Lapitar.log.info("["+name+"] Initializing Pbuffer (assuming "+supersampling+"x supersampling)");
		if (pbuffer != null) {
			destroy();
		}
		pbuffer = new Pbuffer(512*supersampling, 512*supersampling, new PixelFormat(8, 8, 0), null);
		if (pbuffer.isBufferLost())
			throw new LWJGLException("Could not create Pbuffer");
		pbuffer.makeCurrent();
		Lapitar.log.info("["+name+"] Setting up VBOs");
		IntBuffer ids = BufferUtils.createIntBuffer(3);
		glGenBuffersARB(ids);
		vbo = ids.get();
		cvbo = ids.get();
		ibo = ids.get();
		Util.checkGLError();
		
		FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
		vertexBuffer.put(vertices);
		glBindBufferARB(GL_ARRAY_BUFFER_ARB, vbo);
		glBufferDataARB(GL_ARRAY_BUFFER_ARB, vertexBuffer, GL_STATIC_DRAW_ARB);
		Util.checkGLError();

		FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(colors.length);
		colorBuffer.put(colors);
		glBindBufferARB(GL_ARRAY_BUFFER_ARB, cvbo);
		glBufferDataARB(GL_ARRAY_BUFFER_ARB, colorBuffer, GL_STATIC_DRAW_ARB);
		Util.checkGLError();
		
		ByteBuffer indexBuffer = BufferUtils.createByteBuffer(indices.length);
		indexBuffer.put(indices);
		glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER_ARB, ibo);
		glBufferDataARB(GL_ELEMENT_ARRAY_BUFFER_ARB, indexBuffer, GL_STATIC_DRAW_ARB);
		Util.checkGLError();
		
		Lapitar.log.info("["+name+"] Initializing cubes");
		initCubes();
		initialized = true;
	}
	public boolean isInitialized() {
		return initialized;
	}
	protected abstract void initCubes();
	protected void initGL(float width, float height) throws LWJGLException {
		pbuffer.makeCurrent();
		glClearColor(0, 0, 0, 1);
		glClearDepth(1.0);
		glShadeModel(GL_SMOOTH);
		//glEnable(GL_DEPTH_TEST);
		//glDepthFunc(GL_LEQUAL);
		
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		GLU.gluPerspective(
				45.0f,
				width / height,
				0.1f,
				100.0f);
		glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
		glMatrixMode(GL_MODELVIEW);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		initCubes(); // TODO remove, this method should only be called during first init
	}
	public void finish() throws LWJGLException {
		pbuffer.releaseContext();
	}
}
