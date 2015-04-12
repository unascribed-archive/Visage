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
		shadow.rotX = -25;
		shadow.rotY = 45;
		shadow.texture = shadowTexture;
		shadow.lit = false;
		addPrimitive(shadow);
		head = new Cube();
		head.x = 0;
		head.y = -0.3f;
		head.z = -5.05f;
		head.rotX = -25;
		head.rotY = 45;
		addPrimitive(head);
		/*helm = new Cube();
		helm.scaleX = helm.scaleY = helm.scaleZ = 1.1f;
		helm.x = 0;
		helm.y = -0.25f;
		helm.z = -5f;
		helm.rotX = -25;
		helm.rotY = 45;
		addPrimitive(helm);*/
	}
}
