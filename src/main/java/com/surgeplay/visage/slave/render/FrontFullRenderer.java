package com.surgeplay.visage.slave.render;

import com.surgeplay.visage.slave.render.primitive.Plane;
import com.surgeplay.visage.slave.render.primitive.Stage;

public class FrontFullRenderer extends Renderer {

	@Override
	protected void initPrimitives() {
		Stage stage = new Stage();
		stage.y = -1.55f;
		stage.z = -10f;
		stage.rotZ = 0;
		stage.rotY = 0;
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
		rarm.x = -1.5f;
		rarm.z = 1f;
		rarm.scaleZ = 1.5f;
		rarm.scaleX = 0.5f;
		rarm.texture = TextureType.RARM_FRONT;
		rarm.alphaMode = AlphaMode.NONE;
		stage.members.add(rarm);
		
		Plane larm = new Plane();
		larm.x = 1.5f;
		larm.z = 1f;
		larm.scaleZ = 1.5f;
		larm.scaleX = 0.5f;
		larm.texture = TextureType.LARM_FRONT;
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
		rarm2.scaleX = 0.55f;
		rarm2.scaleZ = 1.55f;
		rarm2.z = 0.9999f;
		rarm2.x = -1.4999f;
		rarm2.texture = TextureType.RARM2_FRONT;
		rarm2.alphaMode = AlphaMode.MASK;
		stage.members.add(rarm2);
		
		Plane larm2 = new Plane();
		larm2.scaleX = 0.55f;
		larm2.scaleZ = 1.55f;
		larm2.z = 0.9999f;
		larm2.x = 1.4999f;
		larm2.texture = TextureType.LARM2_FRONT;
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
