package com.surgeplay.visage.slave.render;

import com.surgeplay.visage.slave.render.primitive.Plane;
import com.surgeplay.visage.slave.render.primitive.Stage;

public class FaceRenderer extends Renderer {

	@Override
	protected void initPrimitives() {
		Stage stage = new Stage();
		stage.y = 0;
		stage.z = -2.75f;
		stage.rotZ = 0;
		stage.rotY = 0;
		stage.rotX = -90;
		stage.lit = false;
		addPrimitive(stage);
		
		Plane head = new Plane();
		head.y = 0;
		head.z = 0;
		head.texture = TextureType.HEAD_FRONT;
		head.alphaMode = AlphaMode.NONE;
		stage.members.add(head);
		Plane helm = new Plane();
		helm.scaleX = helm.scaleY = helm.scaleZ = 1.05f;
		helm.z = -0.0001f;
		helm.texture = TextureType.HEAD2_FRONT;
		helm.alphaMode = AlphaMode.MASK;
		stage.members.add(helm);
	}

}
