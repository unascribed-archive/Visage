package com.gameminers.visage.slave.render;

import com.gameminers.visage.slave.render.primitive.Cube;
import com.gameminers.visage.slave.render.primitive.Stage;

public class PlayerRenderer extends Renderer {

	@Override
	protected void initPrimitives() {
		float tilt = -10;
		float angle = 20;
		
		Stage stage = new Stage();
		stage.x = 0;
		stage.y = -2.75f;
		stage.z = -10.2f;
		stage.rotX = tilt;
		stage.rotY = angle;
		addPrimitive(stage);
		
		Cube larm = new Cube();
		larm.x = 1.75f;
		larm.y = 2.375f;
		larm.z = -0.1f;
		larm.scaleY = 1.5f;
		larm.scaleZ = 0.5f;
		larm.scaleX = 0.5f;
		larm.rotZ = -10f;
		larm.texture = TextureType.LARM;
		stage.members.add(larm);
		Cube larm2 = new Cube();
		larm2.x = 1.7f;
		larm2.y = 2.35f;
		larm2.z = -0.1f;
		larm2.scaleY = 1.55f;
		larm2.scaleZ = 0.54f;
		larm2.scaleX = 0.55f;
		larm2.rotZ = -10f;
		larm2.texture = TextureType.LARM2;
		stage.members.add(larm2);
		
		Cube lleg = new Cube();
		lleg.x = 0.5f;
		lleg.y = 5.425f;
		lleg.scaleY = 1.5f;
		lleg.scaleZ = 0.5f;
		lleg.scaleX = 0.5f;
		lleg.texture = TextureType.LLEG;
		stage.members.add(lleg);
		
		Cube rleg = new Cube();
		rleg.x = -0.5f;
		rleg.y = 5.425f;
		rleg.scaleY = 1.5f;
		rleg.scaleZ = 0.5f;
		rleg.scaleX = 0.5f;
		rleg.texture = TextureType.RLEG;
		stage.members.add(rleg);
		
		Cube body = new Cube();
		body.y = 2.475f;
		body.scaleY = 1.5f;
		body.scaleZ = 0.5f;
		body.texture = TextureType.BODY;
		stage.members.add(body);
		Cube body2 = new Cube();
		body2.y = 2.5f;
		body2.scaleY = 1.55f;
		body2.scaleZ = 0.55f;
		body2.scaleX = 1.05f;
		body2.texture = TextureType.BODY2;
		stage.members.add(body2);
		
		Cube lleg2 = new Cube();
		lleg2.x = 0.475f;
		lleg2.y = 5.4f;
		lleg2.scaleY = 1.55f;
		lleg2.scaleZ = 0.55f;
		lleg2.scaleX = 0.55f;
		lleg2.texture = TextureType.LLEG2;
		stage.members.add(lleg2);
		
		Cube rleg2 = new Cube();
		rleg2.x = -0.525f;
		rleg2.y = 5.4f;
		rleg2.scaleY = 1.55f;
		rleg2.scaleZ = 0.55f;
		rleg2.scaleX = 0.55f;
		rleg2.texture = TextureType.RLEG2;
		stage.members.add(rleg2);
		
		Cube head = new Cube();
		head.y = -0.025f;
		head.z = -0.025f;
		head.texture = TextureType.HEAD;
		stage.members.add(head);
		Cube helm = new Cube();
		helm.scaleX = helm.scaleY = helm.scaleZ = 1.05f;
		helm.texture = TextureType.HEAD2;
		stage.members.add(helm);
		
		Cube rarm = new Cube();
		rarm.x = -1.75f;
		rarm.y = 2.325f;
		rarm.z = 0.15f;
		rarm.scaleY = 1.5f;
		rarm.scaleZ = 0.5f;
		rarm.scaleX = 0.5f;
		rarm.rotZ = 10f;
		rarm.texture = TextureType.RARM;
		stage.members.add(rarm);
		Cube rarm2 = new Cube();
		rarm2.x = -1.7f;
		rarm2.y = 1.85f;
		rarm2.z = 0.15f;
		rarm2.scaleY = 1.55f;
		rarm2.scaleZ = 0.55f;
		rarm2.scaleX = 0.55f;
		rarm2.rotZ = 10f;
		rarm2.texture = TextureType.RARM2;
		stage.members.add(rarm2);
	}

}
