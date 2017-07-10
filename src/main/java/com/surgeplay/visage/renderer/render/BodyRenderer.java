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
import com.surgeplay.visage.renderer.render.primitive.Group;

public class BodyRenderer extends Renderer {
	
	public BodyRenderer(RenderContext owner) {
		super(owner);
	}
	
	@Override
	protected void initPrimitives(boolean slim, boolean full, boolean flip) {
		float tilt = -10;
		float angle = 20;
		
		Group group = new Group();
		group.x = 0;
		group.y = full ? (flip ? -2.7f : -2.8f) : -1f;
		group.z = full ? -10.35f : -6f;
		group.rotX = tilt;
		group.rotY = angle;
		addPrimitive(group);
		
		if (full || flip) {
			Plane shadow = new Plane();
			shadow.y = full ? (flip ? 6.825f : 7f) : 2.85f;
			shadow.scaleX = 1.85f;
			shadow.scaleZ = flip ? 1.85f : 0.85f;
			shadow.texture = TextureType.ALL;
			shadow.lit = false;
			shadow.alphaMode = AlphaMode.FULL;
			group.members.add(shadow);
		}
		
		Group group2 = new Group();
		if (flip) {
			group2.rotZ = 180;
			group2.y = ((-group.y)*2)+(full ? 0.3f : -0.25f);
		}
		group.members.add(group2);
		
		Cube head = new Cube();
		head.texture = TextureType.HEAD;
		head.alphaMode = AlphaMode.NONE;
		
		Cube head2 = new Cube();
		head2.scaleX = head2.scaleY = head2.scaleZ = 1.05f;
		head2.texture = TextureType.HEAD2;
		head2.alphaMode = AlphaMode.FULL;
		head2.depthMask = false;
		head2.renderPass = 2;
		
		
		
		Cube body = new Cube();
		body.y = 2.5f;
		body.scaleY = 1.5f;
		body.scaleZ = 0.5f;
		body.texture = TextureType.BODY;
		body.alphaMode = AlphaMode.NONE;
		
		Cube body2 = new Cube();
		body2.y = 2.5f;
		body2.scaleY = 1.55f;
		body2.scaleZ = 0.55f;
		body2.scaleX = 1.05f;
		body2.texture = TextureType.BODY2;
		body2.alphaMode = AlphaMode.FULL;
		body2.depthMask = false;
		body2.renderPass = 2;
		

		
		Cube larm = new Cube();
		larm.x = slim ? 1.375f : 1.5f;
		larm.y = 2.5f;
		larm.scaleY = 1.5f;
		larm.scaleZ = 0.5f;
		larm.scaleX = slim ? 0.375f : 0.5f;
		larm.anchorX = -larm.scaleX;
		larm.anchorY = -larm.scaleY;
		larm.rotZ = -10f;
		larm.texture = slim ? TextureType.LARM_SLIM : TextureType.LARM;
		larm.alphaMode = AlphaMode.NONE;
		
		Cube larm2 = new Cube();
		larm2.x = slim ? 1.375f : 1.5f;
		larm2.y = 2.5f;
		larm2.scaleY = 1.55f;
		larm2.scaleZ = 0.55f;
		larm2.scaleX = slim ? 0.425f : 0.55f;
		larm2.anchorX = -larm2.scaleX;
		larm2.anchorY = -larm2.scaleY;
		larm2.rotZ = -10f;
		larm2.texture = slim ? TextureType.LARM2_SLIM : TextureType.LARM2;
		larm2.alphaMode = AlphaMode.FULL;
		larm2.depthMask = false;
		larm2.renderPass = 2;
		
		
		Cube rarm = new Cube();
		rarm.x = slim ? -1.375f : -1.5f;
		rarm.y = 2.5f;
		rarm.scaleY = 1.5f;
		rarm.scaleZ = 0.5f;
		rarm.scaleX = slim ? 0.375f : 0.5f;
		rarm.anchorX = rarm.scaleX;
		rarm.anchorY = -rarm.scaleY;
		rarm.rotZ = 10f;
		rarm.texture = slim ? TextureType.RARM_SLIM : TextureType.RARM;
		rarm.alphaMode = AlphaMode.NONE;
		
		Cube rarm2 = new Cube();
		rarm2.x = slim ? -1.375f : -1.5f;
		rarm2.y = 2.5f;
		rarm2.scaleY = 1.55f;
		rarm2.scaleZ = 0.55f;
		rarm2.scaleX = slim ? 0.425f : 0.55f;
		rarm2.anchorX = rarm2.scaleX;
		rarm2.anchorY = -rarm2.scaleY;
		rarm2.rotZ = 10f;
		rarm2.texture = slim ? TextureType.RARM2_SLIM : TextureType.RARM2;
		rarm2.alphaMode = AlphaMode.FULL;
		rarm2.depthMask = false;
		rarm2.renderPass = 2;
		
		
		Cube lleg = new Cube();
		lleg.x = 0.5f;
		lleg.y = 5.5f;
		lleg.scaleY = 1.5f;
		lleg.scaleZ = 0.5f;
		lleg.scaleX = 0.5f;
		lleg.anchorY = -lleg.scaleY;
		lleg.texture = TextureType.LLEG;
		lleg.alphaMode = AlphaMode.NONE;
		
		Cube lleg2 = new Cube();
		lleg2.x = 0.5f;
		lleg2.y = 5.5f;
		lleg2.scaleY = 1.55f;
		lleg2.scaleZ = 0.55f;
		lleg2.scaleX = 0.55f;
		lleg2.anchorY = -lleg2.scaleY;
		lleg2.texture = TextureType.LLEG2;
		lleg2.alphaMode = AlphaMode.FULL;
		lleg2.depthMask = false;
		lleg2.renderPass = 2;
		
		
		Cube rleg = new Cube();
		rleg.x = -0.5f;
		rleg.y = 5.5f;
		rleg.scaleY = 1.5f;
		rleg.scaleZ = 0.5f;
		rleg.scaleX = 0.5f;
		rleg.anchorY = -rleg.scaleY;
		rleg.texture = TextureType.RLEG;
		rleg.alphaMode = AlphaMode.NONE;
		
		Cube rleg2 = new Cube();
		rleg2.x = -0.5f;
		rleg2.y = 5.5f;
		rleg2.scaleY = 1.55f;
		rleg2.scaleZ = 0.55f;
		rleg2.scaleX = 0.55f;
		rleg2.anchorY = -rleg2.scaleY;
		rleg2.texture = TextureType.RLEG2;
		rleg2.alphaMode = AlphaMode.FULL;
		rleg2.depthMask = false;
		rleg2.renderPass = 2;
		
		group2.members.add(head);
		group2.members.add(body);
		group2.members.add(larm);
		group2.members.add(rarm);
		group2.members.add(lleg);
		group2.members.add(rleg);
		
		group2.members.add(lleg2);
		group2.members.add(rleg2);
		group2.members.add(body2);
		group2.members.add(head2);
		group2.members.add(larm2);
		group2.members.add(rarm2);
	}

}
