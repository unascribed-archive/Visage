package com.gameminers.visage.slave.render;

import com.gameminers.visage.slave.render.primitive.Cube;
import com.gameminers.visage.slave.render.primitive.Plane;
import com.gameminers.visage.slave.render.primitive.Stage;


public class HeadRenderer extends Renderer {
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
		shadow.texture = TextureType.SHADOW;
		shadow.lit = false;
		stage.members.add(shadow);
		Cube head = new Cube();
		head.y = -0.025f;
		head.z = -0.025f;
		head.texture = TextureType.HEAD;
		stage.members.add(head);
		Cube helm = new Cube();
		helm.scaleX = helm.scaleY = helm.scaleZ = 1.05f;
		helm.z = -0f;
		helm.texture = TextureType.HEAD2;
		stage.members.add(helm);
	}
}
