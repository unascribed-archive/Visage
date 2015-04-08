package blue.lapis.lapitar2.slave.render;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;

import blue.lapis.lapitar2.Lapitar;
import blue.lapis.lapitar2.slave.render.primitive.Primitive;

import com.google.common.collect.Lists;

import static org.lwjgl.opengl.ARBVertexBufferObject.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.Util.checkGLError;
import static org.lwjgl.util.glu.GLU.gluPerspective;

public abstract class Renderer {
	public static final float vertices[] = {
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
	public static final float planeVertices[] = {
		-1.0f,  0.0f,  1.0f,
		 1.0f,  0.0f,  1.0f,
		 1.0f,  0.0f, -1.0f,
		-1.0f,  0.0f, -1.0f
	};
	public final String name = getClass().getSimpleName();
	public Pbuffer pbuffer;
	public List<Primitive> prims = Lists.newArrayList();
	public int vbo, planeVbo, texture;
	private boolean initialized = false;
	protected void addPrimitive(Primitive prim) {
		prims.add(prim);
	}
	public void upload(BufferedImage img) throws LWJGLException {
		pbuffer.makeCurrent();
		int width = img.getWidth();
		int height = img.getHeight();
		Lapitar.log.finer("Uploading "+width+"x"+height+" ("+(width*height)+" pixel) image");
		int[] argb = new int[width*height];
		img.getRGB(0, 0, width, height, argb, 0, width);
		ByteBuffer buf = BufferUtils.createByteBuffer(width*height*4);
		buf.asIntBuffer().put(argb);
		buf.position(0);
		glBindTexture(GL_TEXTURE_2D, texture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
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
		vbo = planeVbo = texture = 0;
		prims.clear();
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
		IntBuffer ids = BufferUtils.createIntBuffer(2);
		glGenBuffersARB(ids);
		vbo = ids.get();
		planeVbo = ids.get();
		checkGLError();
		
		texture = glGenTextures();
		checkGLError();
		
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
		glCullFace(GL_BACK);
		checkGLError();
		
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LEQUAL);
		checkGLError();
		
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		checkGLError();
		
		FloatBuffer lightAmbient = BufferUtils.createFloatBuffer(4);
		lightAmbient.put(1f);
		lightAmbient.put(0.9f);
		lightAmbient.put(0.6f);
		lightAmbient.put(1f);
		lightAmbient.flip();
		glLight(GL_LIGHT0, GL_AMBIENT, lightAmbient);
		checkGLError();
		
		FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4);
		lightPosition.put(3f);
		lightPosition.put(-2f);
		lightPosition.put(1f);
		lightPosition.put(1f);
		lightPosition.flip();
		glLight(GL_LIGHT0, GL_POSITION, lightPosition);
		checkGLError();
		
		glEnable(GL_LIGHTING);
		glEnable(GL_LIGHT0);
		checkGLError();
		
		
		Lapitar.log.finer("["+name+"] Initializing primitives");
		initPrimitives();
		initialized = true;
	}
	public boolean isInitialized() {
		return initialized;
	}
	protected abstract void initPrimitives();
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
		
		initPrimitives(); // TODO remove, this method should only be called during first init
	}
	public void finish() throws LWJGLException {
		pbuffer.releaseContext();
	}
}
