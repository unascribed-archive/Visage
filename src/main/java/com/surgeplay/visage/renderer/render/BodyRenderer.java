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

public class BodyRenderer extends Renderer {
	
	public BodyRenderer(RenderContext owner) {
		super(owner);
	}
	
	@Override
	protected void initPrimitives(boolean slim, boolean full, boolean flip) {
		float tilt = -10;
		float angle = flip ? -20 : 20;
		
		Stage stage = new Stage();
		stage.x = 0;
		stage.y = full ? (flip ? -2.7f : -2.8f) : -1f;
		stage.z = full ? -10.35f : -6f;
		stage.rotX = tilt;
		stage.rotY = angle;
		addPrimitive(stage);
		
		if (full || flip) {
			Plane shadow = new Plane();
			shadow.y = full ? (flip ? 6.825f : 7f) : 2.85f;
			shadow.scaleX = 1.85f;
			shadow.scaleZ = flip ? 1.85f : 0.85f;
			shadow.texture = TextureType.ALL;
			shadow.lit = false;
			shadow.alphaMode = AlphaMode.FULL;
			stage.members.add(shadow);
		}
		
		Stage stage2 = new Stage();
		if (flip) {
			stage2.rotZ = 180;
			stage2.y = ((-stage.y)*2)+(full ? 0.3f : -0.25f);
		}
		stage.members.add(stage2);
		
		Cube larm = new Cube();
		larm.x = slim ? 1.625f : 1.75f;
		larm.y = 2.375f;
		larm.z = -0.1f;
		larm.scaleY = 1.5f;
		larm.scaleZ = 0.5f;
		larm.scaleX = slim ? 0.375f : 0.5f;
		larm.rotZ = -10f;
		larm.texture = slim ? TextureType.LARM_SLIM : TextureType.LARM;
		larm.alphaMode = AlphaMode.NONE;
		stage2.members.add(larm);
		Cube larm2 = new Cube();
		larm2.x = slim ? 1.575f : 1.7f;
		larm2.y = 2.35f;
		larm2.z = -0.1f;
		larm2.scaleY = 1.55f;
		larm2.scaleZ = 0.54f;
		larm2.scaleX = slim ? 0.425f : 0.55f;
		larm2.rotZ = -10f;
		larm2.texture = slim ? TextureType.LARM2_SLIM : TextureType.LARM2;
		larm2.alphaMode = AlphaMode.MASK;
		stage2.members.add(larm2);
		
		Cube lleg = new Cube();
		lleg.x = 0.5f;
		lleg.y = 5.475f;
		lleg.scaleY = 1.5f;
		lleg.scaleZ = 0.5f;
		lleg.scaleX = 0.5f;
		lleg.texture = TextureType.LLEG;
		lleg.alphaMode = AlphaMode.NONE;
		stage2.members.add(lleg);
		
		Cube rleg = new Cube();
		rleg.x = -0.5f;
		rleg.y = 5.475f;
		rleg.scaleY = 1.5f;
		rleg.scaleZ = 0.5f;
		rleg.scaleX = 0.5f;
		rleg.texture = TextureType.RLEG;
		rleg.alphaMode = AlphaMode.NONE;
		stage2.members.add(rleg);
		
		Cube body = new Cube();
		body.y = 2.475f;
		body.scaleY = 1.5f;
		body.scaleZ = 0.5f;
		body.texture = TextureType.BODY;
		body.alphaMode = AlphaMode.NONE;
		stage2.members.add(body);
		Cube body2 = new Cube();
		body2.y = 2.5f;
		body2.scaleY = 1.55f;
		body2.scaleZ = 0.55f;
		body2.scaleX = 1.05f;
		body2.texture = TextureType.BODY2;
		body2.alphaMode = AlphaMode.MASK;
		stage2.members.add(body2);
		
		Cube lleg2 = new Cube();
		lleg2.x = 0.475f;
		lleg2.y = 5.4f;
		lleg2.scaleY = 1.55f;
		lleg2.scaleZ = 0.55f;
		lleg2.scaleX = 0.55f;
		lleg2.texture = TextureType.LLEG2;
		lleg2.alphaMode = AlphaMode.MASK;
		stage2.members.add(lleg2);
		
		Cube rleg2 = new Cube();
		rleg2.x = -0.525f;
		rleg2.y = 5.4f;
		rleg2.scaleY = 1.55f;
		rleg2.scaleZ = 0.55f;
		rleg2.scaleX = 0.55f;
		rleg2.texture = TextureType.RLEG2;
		rleg2.alphaMode = AlphaMode.MASK;
		stage2.members.add(rleg2);
		
		Cube head = new Cube();
		head.y = -0.025f;
		head.z = -0.025f;
		head.texture = TextureType.HEAD;
		head.alphaMode = AlphaMode.NONE;
		stage2.members.add(head);
		Cube helm = new Cube();
		helm.scaleX = helm.scaleY = helm.scaleZ = 1.05f;
		helm.texture = TextureType.HEAD2;
		helm.alphaMode = AlphaMode.MASK;
		stage2.members.add(helm);
		
		Cube rarm = new Cube();
		rarm.x = slim ? -1.625f : -1.75f;
		rarm.y = 2.325f;
		rarm.z = 0.15f;
		rarm.scaleY = 1.5f;
		rarm.scaleZ = 0.5f;
		rarm.scaleX = slim ? 0.375f : 0.5f;
		rarm.rotZ = 10f;
		rarm.texture = slim ? TextureType.RARM_SLIM : TextureType.RARM;
		rarm.alphaMode = AlphaMode.NONE;
		stage2.members.add(rarm);
		Cube rarm2 = new Cube();
		rarm2.x = slim ? -1.625f : -1.7f;
		rarm2.y = 2.3f;
		rarm2.z = 0.15f;
		rarm2.scaleY = 1.55f;
		rarm2.scaleZ = 0.55f;
		rarm2.scaleX = slim ? 0.375f : 0.55f;
		rarm2.rotZ = 10f;
		rarm2.texture = slim ? TextureType.RARM2_SLIM : TextureType.RARM2;
		rarm2.alphaMode = AlphaMode.MASK;
		stage2.members.add(rarm2);
	}

}
