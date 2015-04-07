package blue.lapis.lapitar2.slave.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.Util;

import blue.lapis.lapitar2.Lapitar;

import com.google.common.collect.Lists;

import static org.lwjgl.opengl.ARBVertexBufferObject.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.gluPerspective;

public abstract class Renderer {
	protected static final float vertices[] = {
		// Front
		-1.0f, -1.0f,  1.0f,
		 1.0f, -1.0f,  1.0f,
		 1.0f,  1.0f,  1.0f,
		-1.0f,  1.0f,  1.0f,
		// Back
		-1.0f, -1.0f, -1.0f,
		 1.0f, -1.0f, -1.0f,
		 1.0f,  1.0f, -1.0f,
		-1.0f,  1.0f, -1.0f,
		// Top
		-1.0f,  1.0f,  1.0f,
		 1.0f,  1.0f,  1.0f,
		 1.0f,  1.0f, -1.0f,
		-1.0f,  1.0f, -1.0f,
		// Bottom
		-1.0f, -1.0f, -1.0f,
		 1.0f, -1.0f, -1.0f,
		 1.0f, -1.0f,  1.0f,
		-1.0f, -1.0f,  1.0f,
		// Left
		 1.0f, -1.0f,  1.0f,
		 1.0f, -1.0f, -1.0f,
		 1.0f,  1.0f, -1.0f,
		 1.0f,  1.0f,  1.0f,
		// Right
		-1.0f, -1.0f, -1.0f,
		-1.0f, -1.0f,  1.0f,
		-1.0f,  1.0f,  1.0f,
		-1.0f,  1.0f, -1.0f
	};
	protected final String name = getClass().getSimpleName();
	protected Pbuffer pbuffer;
	protected List<Cube> cubes = Lists.newArrayList();
	protected int vbo, texture;
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
		vbo = texture = 0;
		cubes.clear();
		initialized = false;
	}
	public void init(int supersampling) throws LWJGLException {
		Lapitar.log.finer("["+name+"] Initializing Pbuffer (assuming "+supersampling+"x supersampling)");
		if (pbuffer != null) {
			destroy();
		}
		pbuffer = new Pbuffer(512*supersampling, 512*supersampling, new PixelFormat(8, 8, 0), null);
		if (pbuffer.isBufferLost())
			throw new LWJGLException("Could not create Pbuffer");
		pbuffer.makeCurrent();
		Lapitar.log.finer("["+name+"] Setting up VBOs");
		IntBuffer ids = BufferUtils.createIntBuffer(1);
		glGenBuffersARB(ids);
		vbo = ids.get();
		Util.checkGLError();
		
		FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
		vertexBuffer.put(vertices);
		vertexBuffer.flip();
		glBindBufferARB(GL_ARRAY_BUFFER_ARB, vbo);
		glBufferDataARB(GL_ARRAY_BUFFER_ARB, vertexBuffer, GL_STATIC_DRAW_ARB);
		Util.checkGLError();
		
		glClearColor(0, 0, 0, 0);
		glClearDepth(1.0);
		glShadeModel(GL_SMOOTH);
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LEQUAL);
		glCullFace(GL_BACK);
		glDisable(GL_LIGHTING);
		Util.checkGLError();
		
		Lapitar.log.finer("["+name+"] Initializing cubes");
		initCubes();
		initialized = true;
	}
	public boolean isInitialized() {
		return initialized;
	}
	protected abstract void initCubes();
	protected void initGL(float width, float height) throws LWJGLException {
		pbuffer.makeCurrent();
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		gluPerspective(
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
