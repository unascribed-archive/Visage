package blue.lapis.lapitar2.slave.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.Util;

public class Cube {

	private FloatBuffer vertexBuffer;
	private ByteBuffer indexBuffer;

	private int texture;
	
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

	public Cube(int textureId) {
		this.texture = textureId;
	}

	public void render(Renderer renderer) {
		GL11.glPushMatrix();
			GL11.glDisable(GL11.GL_TEXTURE_2D);
	
			GL11.glTranslatef(x, y, z);
			GL11.glRotatef(rotX, 1.0f, 0.0f, 0.0f);
			GL11.glRotatef(rotY, 0.0f, 1.0f, 0.0f);
			GL11.glRotatef(rotZ, 0.0f, 0.0f, 1.0f);
			GL11.glScalef(scaleX, scaleY, scaleZ);
			Util.checkGLError();
			
		    GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderer.vbo);
		    GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0);
		    Util.checkGLError();
		     
		    GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderer.cvbo);
		    GL11.glColorPointer(4, GL11.GL_FLOAT, 0, 0);
		    Util.checkGLError();
		    
			GL11.glFrontFace(GL11.GL_CCW);
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, renderer.ibo);
			GL11.glDrawElements(GL11.GL_TRIANGLES, Renderer.indices.length, GL11.GL_UNSIGNED_INT, 0);
			Util.checkGLError();
		GL11.glPopMatrix();
	}
}

