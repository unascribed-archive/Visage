/*
 * Visage
 * Copyright (c) 2015, Aesen Vismea <aesen@gameminers.com>
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
			if (Visage.debug) Visage.log.finer("Creating texture coord buffer");
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

