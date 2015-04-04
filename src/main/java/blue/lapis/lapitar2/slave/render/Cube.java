package blue.lapis.lapitar2.slave.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

public class Cube {

	private FloatBuffer vertexBuffer;
	private FloatBuffer textureBuffer;
	private ByteBuffer indexBuffer;

	private int texture;
	
	private int vbo;
	private int ibo;
	private boolean initialized;

	private float vertices[] = {
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

	private byte indices[] = {
			 0,  1,  3,     0,  3,  2,
			 4,  5,  7,     4,  7,  6,
			 8,  9, 11,     8, 11, 10,
			12, 13, 15,    12, 15, 14,
			16, 17, 19,    16, 19, 18,
			20, 21, 23,    20, 23, 22,
		};

	public float scaleX = 1.0f;
	public float scaleY = 1.0f;
	public float scaleZ = 1.0f;
	public float x, y, z, rotX, rotY, rotZ;

	public Cube(int textureId) {
		this.texture = textureId;
		ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		vertexBuffer = byteBuf.asFloatBuffer();
		vertexBuffer.put(vertices);
		vertexBuffer.position(0);

		indexBuffer = ByteBuffer.allocateDirect(indices.length);
		indexBuffer.put(indices);
		indexBuffer.position(0);
	}

	public void render() {
		GL11.glPushMatrix();
			GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			if (!initialized) {
				vbo = GL15.glGenBuffers();
				ibo = GL15.glGenBuffers();
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
				GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);
				GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
				GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);
				initialized = true;
			}
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			//GL11.glEnable(GL11.GL_TEXTURE_2D);
			//GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
	
			GL11.glTranslatef(x, y, z);
			GL11.glRotatef(rotX, 1.0f, 0.0f, 0.0f);
			GL11.glRotatef(rotY, 0.0f, 1.0f, 0.0f);
			GL11.glRotatef(rotZ, 0.0f, 0.0f, 1.0f);
			GL11.glScalef(scaleX, scaleY, scaleZ);
	
			GL11.glFrontFace(GL11.GL_CCW);
	
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
			//GL11.glTexCoordPointer(uv.length, 2, textureBuffer);
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
	
			GL11.glDrawElements(GL11.GL_TRIANGLES, indices.length, GL11.GL_UNSIGNED_INT, 0);
	
			GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
			GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL11.glPopMatrix();
	}
}

