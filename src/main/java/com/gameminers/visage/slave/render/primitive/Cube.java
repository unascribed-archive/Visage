package com.gameminers.visage.slave.render.primitive;

import static org.lwjgl.opengl.ARBVertexBufferObject.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import com.gameminers.visage.Visage;
import com.gameminers.visage.slave.render.Renderer;

public class Cube extends Primitive {
	private int tcbo = Integer.MAX_VALUE;
	
	@Override
	public void render(Renderer renderer) {
		if (tcbo == Integer.MAX_VALUE) {
			Visage.log.finer("Creating texture coord buffer");
			tcbo = glGenBuffersARB();
			FloatBuffer uv = BufferUtils.createFloatBuffer(texture.u.length+texture.v.length);
			for (int i = 0; i < texture.u.length; i++) {
				uv.put(texture.u[i]);
				uv.put(texture.v[i]);
			}
			uv.flip();
			glBindBufferARB(GL_ARRAY_BUFFER_ARB, tcbo);
			glBufferDataARB(GL_ARRAY_BUFFER_ARB, uv, GL_STATIC_DRAW_ARB);
		}
		doRender(renderer, renderer.vbo, tcbo, Renderer.vertices);
	}
}

