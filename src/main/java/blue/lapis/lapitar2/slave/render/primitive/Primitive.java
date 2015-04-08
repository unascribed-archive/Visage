package blue.lapis.lapitar2.slave.render.primitive;

import static org.lwjgl.opengl.ARBVertexBufferObject.*;
import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.opengl.Util;

import blue.lapis.lapitar2.Lapitar;
import blue.lapis.lapitar2.slave.render.Renderer;

public abstract class Primitive {
	public float scaleX = 1.0f;
	public float scaleY = 1.0f;
	public float scaleZ = 1.0f;
	public float r = 1.0f;
	public float g = 1.0f;
	public float b = 1.0f;
	public float a = 1.0f;
	public float x, y, z, rotX, rotY, rotZ;
	public boolean lit = true;
	public boolean textured = true;
	public abstract void render(Renderer renderer);
	protected void doRender(Renderer renderer, int vbo, float[] vertices) {
		glPushMatrix();
			Lapitar.log.finest("Rendering "+getClass().getSimpleName());
			Lapitar.log.finest("Translating to "+x+", "+y+", "+z);
			glTranslatef(x, y, z);
			Lapitar.log.finest("Rotating by "+rotX+"°, "+rotY+"°, "+rotZ+"°");
			glRotatef(rotX, 1.0f, 0.0f, 0.0f);
			glRotatef(rotY, 0.0f, 1.0f, 0.0f);
			glRotatef(rotZ, 0.0f, 0.0f, 1.0f);
			Lapitar.log.finest("Scaling by "+scaleX+"x, "+scaleY+"x, "+scaleZ+"x");
			glScalef(scaleX, scaleY, scaleZ);
			Lapitar.log.finest("Coloring as "+r+", "+g+", "+b+", "+a);
			glColor4f(r, g, b, a);
			if (lit) {
				Lapitar.log.finest("Enabling lighting");
				glEnable(GL_LIGHTING);
			} else {
				Lapitar.log.finest("Disabling lighting");
				glDisable(GL_LIGHTING);
			}
			if (textured) {
				Lapitar.log.finest("Enabling texturing");
				glEnable(GL_TEXTURE_2D);
			} else {
				Lapitar.log.finest("Disabling texturing");
				glDisable(GL_TEXTURE_2D);
			}
			Util.checkGLError();
			
			Lapitar.log.finest("Setting VBO");
		    glEnableClientState(GL_VERTEX_ARRAY);
		    glBindBufferARB(GL_ARRAY_BUFFER_ARB, vbo);
		    glVertexPointer(3, GL_FLOAT, 0, 0);
		    Util.checkGLError();
		    
		    Lapitar.log.finest("Rendering");
			glDrawArrays(GL_QUADS, 0, vertices.length/3);
			Util.checkGLError();
			/*glBegin(GL_QUADS);
			for (int i = 0; i < vertices.length/3; i++) {
				int idx = i*3;
				float x = vertices[idx];
				float y = vertices[idx+1];
				float z = vertices[idx+2];
				Lapitar.log.finest("Vertex "+x+", "+y+", "+z);
				glVertex3f(x, y, z);
				glColor3f((float)Math.random(), (float)Math.random(), (float)Math.random());
			}
			glEnd();*/
		Util.checkGLError();
	glPopMatrix();
	}
}
