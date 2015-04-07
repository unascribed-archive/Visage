package blue.lapis.lapitar2.slave.render;

import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.opengl.Util;

import blue.lapis.lapitar2.Lapitar;

public class Cube {

	private float[] uv = {
			// Front
			0.25f, 1.00f,
			0.50f, 1.00f,
			0.25f, 0.50f,
			0.50f, 0.50f,
			// Left
			0.50f, 1.00f,
			0.75f, 1.00f,
			0.50f, 0.50f,
			0.75f, 0.50f,
			// Back
			0.75f, 1.00f,
			1.00f, 1.00f,
			0.75f, 0.50f,
			1.00f, 0.50f,
			// Right
			0.00f, 1.00f,
			0.25f, 1.00f,
			0.00f, 0.50f,
			0.25f, 0.50f,
			// Bottom
			0.50f, 0.50f,
			0.75f, 0.50f,
			0.50f, 0.00f,
			0.75f, 0.00f,
			// Top
			0.25f, 0.50f,
			0.50f, 0.50f,
			0.25f, 0.00f,
			0.50f, 0.00f,

	};

	public float scaleX = 1.0f;
	public float scaleY = 1.0f;
	public float scaleZ = 1.0f;
	public float x, y, z, rotX, rotY, rotZ;

	public void render(Renderer renderer) {
		glPushMatrix();
			glDisable(GL_TEXTURE_2D);
			glDisable(GL_LIGHTING);
			glEnable(GL_DEPTH_TEST);
	
			Lapitar.log.finest("Translating cube to "+x+", "+y+", "+z);
			glTranslatef(x, y, z);
			Lapitar.log.finest("Rotating cube by "+rotX+"°, "+rotY+"°, "+rotZ+"°");
			glRotatef(rotX, 1.0f, 0.0f, 0.0f);
			glRotatef(rotY, 0.0f, 1.0f, 0.0f);
			glRotatef(rotZ, 0.0f, 0.0f, 1.0f);
			Lapitar.log.finest("Scaling cube by "+scaleX+"x, "+scaleY+"x, "+scaleZ+"x");
			glScalef(scaleX, scaleY, scaleZ);
			Util.checkGLError();
			
			/*Lapitar.log.finest("Setting VBO");
		    glEnableClientState(GL_VERTEX_ARRAY);
		    glBindBufferARB(GL_ARRAY_BUFFER_ARB, renderer.vbo);
		    glVertexPointer(3, GL_FLOAT, 0, 0);
		    Util.checkGLError();
		    
		    Lapitar.log.finest("Rendering");
			glDrawArrays(GL_QUADS, 0, Renderer.vertices.length);
			Util.checkGLError();*/
			glFrontFace(GL_CW);
			glBegin(GL_QUADS);
			for (int i = 0; i < Renderer.vertices.length/3; i++) {
				int idx = i*3;
				float x = Renderer.vertices[idx];
				float y = Renderer.vertices[idx+1];
				float z = Renderer.vertices[idx+2];
				Lapitar.log.finest("Vertex "+x+", "+y+", "+z);
				glVertex3f(x, y, z);
				glColor3f((float)Math.random(), (float)Math.random(), (float)Math.random());
			}
			glEnd();
			Util.checkGLError();
		glPopMatrix();
	}
}

