package com.gameminers.visage.slave.render.primitive;

import static org.lwjgl.opengl.GL11.*;

import java.util.List;

import com.gameminers.visage.Visage;
import com.gameminers.visage.slave.render.Renderer;
import com.google.common.collect.Lists;

public class Stage extends Primitive {
	public final List<Primitive> members = Lists.newArrayList();
	@Override
	public void render(Renderer renderer) {
		glPushMatrix();
			if (Visage.trace) Visage.log.finest("Rendering "+getClass().getSimpleName());
			if (Visage.trace) Visage.log.finest("Translating to "+x+", "+y+", "+z);
			glTranslatef(x, y, z);
			if (Visage.trace) Visage.log.finest("Rotating by "+rotX+"°, "+rotY+"°, "+rotZ+"°");
			glRotatef(rotX, 1.0f, 0.0f, 0.0f);
			glRotatef(rotY, 0.0f, 1.0f, 0.0f);
			glRotatef(rotZ, 0.0f, 0.0f, 1.0f);
			if (Visage.trace) Visage.log.finest("Scaling by "+scaleX+"x, "+scaleY+"x, "+scaleZ+"x");
			glScalef(scaleX, scaleY, scaleZ);
			
			if (lit) {
				if (Visage.trace) Visage.log.finest("Enabling lighting");
				glEnable(GL_LIGHTING);
				renderer.lightPosition.position(0);
				glLight(GL_LIGHT0, GL_POSITION, renderer.lightPosition);
			} else {
				if (Visage.trace) Visage.log.finest("Disabling lighting");
				glDisable(GL_LIGHTING);
			}
			
			if (Visage.trace) Visage.log.finest("Rendering");
			for (Primitive p : members) {
				p.inStage = true;
				p.render(renderer);
			}
		glPopMatrix();
	}

}
