package blue.lapis.lapitar2.slave.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.ARBVertexBufferObject.*;
import org.lwjgl.opengl.Util;

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
	
			glTranslatef(x, y, z);
			glRotatef(rotX, 1.0f, 0.0f, 0.0f);
			glRotatef(rotY, 0.0f, 1.0f, 0.0f);
			glRotatef(rotZ, 0.0f, 0.0f, 1.0f);
			glScalef(scaleX, scaleY, scaleZ);
			Util.checkGLError();
			
		    glEnableClientState(GL_VERTEX_ARRAY);
		    glBindBufferARB(GL_ARRAY_BUFFER_ARB, renderer.vbo);
		    glVertexPointer(3, GL_FLOAT, 0, 0);
		    Util.checkGLError();
		     
		    glEnableClientState(GL_COLOR_ARRAY);
		    glBindBufferARB(GL_ARRAY_BUFFER_ARB, renderer.cvbo);
		    glColorPointer(4, GL_FLOAT, 0, 0);
		    Util.checkGLError();
		    
			glFrontFace(GL_CCW);
			glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER_ARB, renderer.ibo);
			glDrawElements(GL_TRIANGLES, Renderer.indices.length, GL_UNSIGNED_INT, 0);
			Util.checkGLError();
		glPopMatrix();
	}
}

