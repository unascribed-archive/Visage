package com.gameminers.visage.slave.render;

import com.gameminers.visage.Visage;
import com.gameminers.visage.slave.render.primitive.Cube;
import com.gameminers.visage.slave.render.primitive.Plane;


public class HeadRenderer extends Renderer {
	private Cube helm, head;
	private Plane shadow;
	@Override
	protected void initPrimitives() {
		Visage.log.info("initPrimitives");
		prims.clear();
		shadow = new Plane();
		shadow.x = 0f;
		shadow.y = 0.7f;
		shadow.z = -6.5f;
		shadow.scaleX = shadow.scaleZ = 2.3f;
		shadow.rotX = -20;
		shadow.rotY = -35;
		shadow.texture = TextureType.SHADOW;
		shadow.lit = false;
		addPrimitive(shadow);
		head = new Cube();
		head.x = 0;
		head.y = -0.275f;
		head.z = -5.025f;
		head.rotX = -20;
		head.rotY = -35;
		head.texture = TextureType.HEAD;
		addPrimitive(head);
		helm = new Cube();
		helm.scaleX = helm.scaleY = helm.scaleZ = 1.05f;
		helm.x = 0;
		helm.y = -0.25f;
		helm.z = -5f;
		helm.rotX = -20;
		helm.rotY = -35;
		helm.texture = TextureType.HEAD2;
		addPrimitive(helm);
	}
}
