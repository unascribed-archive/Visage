/*
 * The MIT License
 *
 * Copyright (c) 2015-2017, William Thompson (unascribed)
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
import com.surgeplay.visage.renderer.render.primitive.Cube;
import com.surgeplay.visage.renderer.render.primitive.Plane;
import com.surgeplay.visage.renderer.render.primitive.Stage;


public class HeadRenderer extends Renderer {
	public HeadRenderer(RenderContext owner) {
		super(owner);
	}

	@Override
	protected void initPrimitives() {
		float tilt = -20;
		float angle = -35;
		
		Stage stage = new Stage();
		stage.y = -0.25f;
		stage.z = -5f;
		stage.rotX = tilt;
		stage.rotY = angle;
		addPrimitive(stage);
		
		Plane shadow = new Plane();
		shadow.y = 1;
		shadow.scaleX = shadow.scaleZ = 1.95f;
		shadow.texture = TextureType.ALL;
		shadow.lit = false;
		stage.members.add(shadow);
		Cube head = new Cube();
		head.y = -0.025f;
		head.z = -0.025f;
		head.texture = TextureType.HEAD;
		head.alphaMode = AlphaMode.NONE;
		stage.members.add(head);
		Cube helm = new Cube();
		helm.scaleX = helm.scaleY = helm.scaleZ = 1.05f;
		helm.z = -0f;
		helm.texture = TextureType.HEAD2;
		helm.alphaMode = AlphaMode.MASK;
		stage.members.add(helm);
	}
}
