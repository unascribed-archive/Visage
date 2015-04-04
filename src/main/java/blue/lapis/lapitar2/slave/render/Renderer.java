package blue.lapis.lapitar2.slave.render;

import java.util.List;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.glu.GLU;

import com.google.common.collect.Lists;

public abstract class Renderer {
	protected Pbuffer pbuffer;
	protected List<Cube> cubes = Lists.newArrayList();
	protected void addCube(Cube cube) {
		cubes.add(cube);
	}
	public void render(int width, int height) throws LWJGLException {
		initGL(width, height);
		for (Cube cube : cubes) {
			cube.render();
		}
	}
	public void destroy() {
		if (pbuffer != null) {
			pbuffer.destroy();
			pbuffer = null;
		}
	}
	public void init(int supersampling) throws LWJGLException {
		if (pbuffer != null) {
			destroy();
		}
		pbuffer = new Pbuffer(512*supersampling, 512*supersampling, new PixelFormat(8, 8, 0), null);
		if (pbuffer.isBufferLost())
			throw new LWJGLException("Could not create Pbuffer");
		pbuffer.makeCurrent();
		initCubes();
	}
	public boolean isInitialized() {
		return pbuffer != null;
	}
	protected abstract void initCubes();
	protected void initGL(float width, float height) throws LWJGLException {
		pbuffer.makeCurrent();
		GL11.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
		GL11.glClearDepth(1.0);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GLU.gluPerspective(
				45.0f,
				width / height,
				0.1f,
				100.0f);
		GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		initCubes();
	}
	public void finish() throws LWJGLException {
		pbuffer.releaseContext();
	}
}
