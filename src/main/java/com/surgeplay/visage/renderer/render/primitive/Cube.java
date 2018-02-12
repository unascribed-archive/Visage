/*
 * The MIT License
 *
 * Copyright (c) 2015-2018, Una Thompson (unascribed)
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

package com.surgeplay.visage.renderer.render.primitive;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL15.*;

import com.surgeplay.visage.Visage;
import com.surgeplay.visage.renderer.RenderContext;
import com.surgeplay.visage.renderer.render.Renderer;

public class Cube extends Primitive {
	private int tcbo = Integer.MAX_VALUE;
	
	@Override
	public void render(Renderer renderer) {
		if (tcbo == Integer.MAX_VALUE) {
			if (Visage.trace) Visage.log.finest("Creating texture coord buffer");
			tcbo = glGenBuffers();
			FloatBuffer uv = BufferUtils.createFloatBuffer(texture.u.length+texture.v.length);
			for (int i = 0; i < texture.u.length; i++) {
				uv.put(texture.u[i]);
				uv.put(texture.v[i]);
			}
			uv.flip();
			glBindBuffer(GL_ARRAY_BUFFER, tcbo);
			glBufferData(GL_ARRAY_BUFFER, uv, GL_STATIC_DRAW);
		}
		doRender(renderer, renderer.owner.cubeVbo, tcbo, RenderContext.vertices);
	}
}

