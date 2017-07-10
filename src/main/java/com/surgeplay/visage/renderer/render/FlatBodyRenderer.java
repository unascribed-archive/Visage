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
import com.surgeplay.visage.renderer.render.primitive.Plane;
import com.surgeplay.visage.renderer.render.primitive.Stage;

public class FlatBodyRenderer extends Renderer {

	public FlatBodyRenderer(RenderContext owner) {
		super(owner);
	}

	@Override
	protected void initPrimitives(boolean slim, boolean full, boolean flip) {
		Stage stage = new Stage();
		if (full) {
			stage.y = flip ? 1.5f : -1.5f;
			stage.z = -9.75f;
		} else {
			stage.y = flip ? 0.04f : -0.04f;
			stage.z = -6.25f;
		}
		stage.rotZ = 0;
		stage.rotY = flip ? 180 : 0;
		stage.rotX = -90;
		stage.lit = false;
		addPrimitive(stage);
		
		Plane head = new Plane();
		head.x = 0;
		head.z = -1.5f;
		head.texture = TextureType.HEAD_FRONT;
		head.alphaMode = AlphaMode.NONE;
		stage.members.add(head);
		
		Plane body = new Plane();
		body.x = 0;
		body.z = 1f;
		body.scaleZ = 1.5f;
		body.texture = TextureType.BODY_FRONT;
		body.alphaMode = AlphaMode.NONE;
		stage.members.add(body);
		
		Plane rarm = new Plane();
		rarm.x = slim ? -1.375f : -1.5f;
		rarm.z = 1f;
		rarm.scaleZ = 1.5f;
		rarm.scaleX = slim ? 0.375f : 0.5f;
		rarm.texture = slim ? TextureType.RARM_SLIM_FRONT : TextureType.RARM_FRONT;
		rarm.alphaMode = AlphaMode.NONE;
		stage.members.add(rarm);
		
		Plane larm = new Plane();
		larm.x = slim ? 1.375f : 1.5f;
		larm.z = 1f;
		larm.scaleZ = 1.5f;
		larm.scaleX = slim ? 0.375f : 0.5f;
		larm.texture = slim ? TextureType.LARM_SLIM_FRONT : TextureType.LARM_FRONT;
		larm.alphaMode = AlphaMode.NONE;
		stage.members.add(larm);
		
		Plane lleg = new Plane();
		lleg.x = 0.5f;
		lleg.z = 4f;
		lleg.scaleZ = 1.5f;
		lleg.scaleX = 0.5f;
		lleg.texture = TextureType.LLEG_FRONT;
		lleg.alphaMode = AlphaMode.NONE;
		stage.members.add(lleg);
		
		Plane rleg = new Plane();
		rleg.x = -0.5f;
		rleg.z = 4f;
		rleg.scaleZ = 1.5f;
		rleg.scaleX = 0.5f;
		rleg.texture = TextureType.RLEG_FRONT;
		rleg.alphaMode = AlphaMode.NONE;
		stage.members.add(rleg);

		
		
		Plane helm = new Plane();
		helm.scaleX = helm.scaleY = helm.scaleZ = 1.05f;
		helm.z = -1.5001f;
		helm.texture = TextureType.HEAD2_FRONT;
		helm.alphaMode = AlphaMode.MASK;
		stage.members.add(helm);
		
		Plane body2 = new Plane();
		body2.scaleX = body2.scaleY = 1.05f;
		body2.scaleZ = 1.55f;
		body2.z = 0.9999f;
		body2.texture = TextureType.BODY2_FRONT;
		body2.alphaMode = AlphaMode.MASK;
		stage.members.add(body2);
		
		Plane rarm2 = new Plane();
		rarm2.scaleX = slim ? 0.425f : 0.55f;
		rarm2.scaleZ = 1.55f;
		rarm2.z = 0.9999f;
		rarm2.x = slim ? -1.375f : -1.5f;
		rarm2.texture = slim ? TextureType.RARM2_SLIM_FRONT : TextureType.RARM2_FRONT;
		rarm2.alphaMode = AlphaMode.MASK;
		stage.members.add(rarm2);
		
		Plane larm2 = new Plane();
		larm2.scaleX = slim ? 0.425f : 0.55f;
		larm2.scaleZ = 1.55f;
		larm2.z = 0.9999f;
		larm2.x = slim ? 1.375f : 1.5f;
		larm2.texture = slim ? TextureType.LARM2_SLIM_FRONT : TextureType.LARM2_FRONT;
		larm2.alphaMode = AlphaMode.MASK;
		stage.members.add(larm2);
		
		Plane lleg2 = new Plane();
		lleg2.x = 0.4999f;
		lleg2.z = 3.9999f;
		lleg2.scaleZ = 1.55f;
		lleg2.scaleX = 0.55f;
		lleg2.texture = TextureType.LLEG2_FRONT;
		lleg2.alphaMode = AlphaMode.MASK;
		stage.members.add(lleg2);
		
		Plane rleg2 = new Plane();
		rleg2.x = -0.4999f;
		rleg2.z = 3.9999f;
		rleg2.scaleZ = 1.55f;
		rleg2.scaleX = 0.55f;
		rleg2.texture = TextureType.RLEG2_FRONT;
		rleg2.alphaMode = AlphaMode.MASK;
		stage.members.add(rleg2);
	}

}
