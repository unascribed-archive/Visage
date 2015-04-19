package com.gameminers.visage.slave.render.primitive;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.ARBVertexBufferObject.*;

import org.lwjgl.opengl.Util;

import com.gameminers.visage.Visage;
import com.gameminers.visage.slave.render.Renderer;
import com.gameminers.visage.slave.render.TextureType;

public abstract class Primitive {
	public float scaleX = 1.0f;
	public float scaleY = 1.0f;
	public float scaleZ = 1.0f;
	public float x, y, z, rotX, rotY, rotZ;
	public boolean lit = true;
	public boolean textured = true;
	public TextureType texture = TextureType.NONE;
	protected boolean inStage = true;
	public abstract void render(Renderer renderer);
	protected void doRender(Renderer renderer, int vbo, int tcbo, float[] vertices) {
		glPushMatrix();
			if (Visage.trace) Visage.log.finest("Rendering "+getClass().getSimpleName());
			if (Visage.trace) Visage.log.finest("Translating to "+x+", "+y+", "+z);
			glTranslatef(x, y, z);
			if (Visage.trace) Visage.log.finest("Rotating by "+rotX+"°, "+rotY+"°, "+rotZ+"°");
			glRotatef(rotX, 1.0f, 0.0f, 0.0f);
			glRotatef(rotY, 0.0f, 1.0f, 0.0f);
			glRotatef(rotZ, 0.0f, 0.0f, 1.0f);
			if (Visage.trace) Visage.log.finest("Scaling by "+scaleX+"x, "+scaleY+"x, "+scaleZ+"x");
			glScalef(scaleX, scaleY*-1, scaleZ);
			if (!inStage && lit) {
				if (Visage.trace) Visage.log.finest("Enabling lighting");
				glEnable(GL_LIGHTING);
				renderer.lightPosition.position(0);
				glLight(GL_LIGHT0, GL_POSITION, renderer.lightPosition);
			} else if (!inStage) {
				if (Visage.trace) Visage.log.finest("Disabling lighting");
				glDisable(GL_LIGHTING);
			}
			if (textured) {
				if (Visage.trace) Visage.log.finest("Enabling texturing - texture "+texture);
				glEnable(GL_TEXTURE_2D);
				glBindTexture(GL_TEXTURE_2D, texture == TextureType.SHADOW ? renderer.shadowTexture : renderer.texture);
			} else {
				if (Visage.trace) Visage.log.finest("Disabling texturing");
				glDisable(GL_TEXTURE_2D);
			}
			Util.checkGLError();
			
			if (Visage.trace) Visage.log.finest("Setting VBO");
    		glEnableClientState(GL_VERTEX_ARRAY);
    		glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    		if (tcbo == Integer.MAX_VALUE) {
        		glBindBufferARB(GL_ARRAY_BUFFER_ARB, vbo);
    			glTexCoordPointer(2, GL_FLOAT, 20, 12);
    			glVertexPointer(3, GL_FLOAT, 20, 0);
    		} else {
    			glEnableClientState(GL_NORMAL_ARRAY);
    			glBindBufferARB(GL_ARRAY_BUFFER_ARB, tcbo);
    			glTexCoordPointer(2, GL_FLOAT, 0, 0);
    			glBindBufferARB(GL_ARRAY_BUFFER_ARB, vbo);
    			glVertexPointer(3, GL_FLOAT, 24, 0);
    			glNormalPointer(GL_FLOAT, 24, 12);
    		}
		    Util.checkGLError();
		    
		    if (Visage.trace) Visage.log.finest("Rendering");
		    if (tcbo == Integer.MAX_VALUE) {
		    	glDrawArrays(GL_QUADS, 0, vertices.length/5);
		    } else {
		    	glDrawArrays(GL_QUADS, 0, vertices.length/6);
		    }
			Util.checkGLError();
			
			/*glBegin(GL_QUADS);
			for (int i = 0; i < vertices.length/3; i++) {
				int idx = i*3;
				float x = vertices[idx];
				float y = vertices[idx+1];
				float z = vertices[idx+2];
				float u = texture.u[i];
				float v = texture.v[i];
				if (Visage.trace) Visage.log.finest("Vertex "+x+", "+y+", "+z);
				if (Visage.trace) Visage.log.finest("Texcoord "+u+", "+v);
				glNormal3f(-0.2f, 0, -1);
				glTexCoord2f(u, v);
				glVertex3f(x, y, z);
			}
			glEnd();*/
		glPopMatrix();
	}
}
