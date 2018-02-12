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

package com.surgeplay.visage.renderer.render;

import com.surgeplay.visage.renderer.RenderContext;
import com.surgeplay.visage.renderer.render.primitive.Plane;
import com.surgeplay.visage.renderer.render.primitive.Group;

public class FaceRenderer extends Renderer {

	public FaceRenderer(RenderContext owner) {
		super(owner);
	}

	@Override
	protected void initPrimitives(boolean slim, boolean full, boolean flip) {
		Group stage = new Group();
		stage.y = 0;
		stage.z = -2.5f;
		stage.rotZ = 0;
		stage.rotY = flip ? 180 : 0;
		stage.rotX = -90;
		stage.lit = false;
		addPrimitive(stage);
		
		Plane head = new Plane();
		head.y = 0;
		head.z = 0;
		head.texture = TextureType.HEAD_FRONT;
		stage.members.add(head);
		Plane helm = new Plane();
		helm.scaleX = helm.scaleY = helm.scaleZ = 1.05f;
		helm.texture = TextureType.HEAD2_FRONT;
		helm.depthMask = false;
		stage.members.add(helm);
	}

}
