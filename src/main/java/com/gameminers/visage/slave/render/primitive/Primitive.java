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
	public abstract void render(Renderer renderer);
	protected void doRender(Renderer renderer, int vbo, int tcbo, float[] vertices) {
		glPushMatrix();
			Visage.log.finest("Rendering "+getClass().getSimpleName());
			Visage.log.finest("Translating to "+x+", "+y+", "+z);
			glTranslatef(x, y, z);
			Visage.log.finest("Rotating by "+rotX+"°, "+rotY+"°, "+rotZ+"°");
			glRotatef(rotX, 1.0f, 0.0f, 0.0f);
			glRotatef(rotY, 0.0f, 1.0f, 0.0f);
			glRotatef(rotZ, 0.0f, 0.0f, 1.0f);
			Visage.log.finest("Scaling by "+scaleX+"x, "+scaleY+"x, "+scaleZ+"x");
			glScalef(scaleX, scaleY*-1, scaleZ);
			if (lit) {
				Visage.log.finest("Enabling lighting");
				glEnable(GL_LIGHTING);
			} else {
				Visage.log.finest("Disabling lighting");
				glDisable(GL_LIGHTING);
			}
			if (textured) {
				Visage.log.finest("Enabling texturing - texture "+texture);
				glEnable(GL_TEXTURE_2D);
				glBindTexture(GL_TEXTURE_2D, texture == TextureType.SHADOW ? renderer.shadowTexture : renderer.texture);
			} else {
				Visage.log.finest("Disabling texturing");
				glDisable(GL_TEXTURE_2D);
			}
			Util.checkGLError();
			
			Visage.log.finest("Setting VBO");
    		Util.checkGLError();
    		
    		glEnableClientState(GL_VERTEX_ARRAY);
    		glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    		if (tcbo == Integer.MAX_VALUE) {
        		glBindBufferARB(GL_ARRAY_BUFFER_ARB, vbo);
    			glTexCoordPointer(2, GL_FLOAT, 20, 12);
    			glVertexPointer(3, GL_FLOAT, 20, 0);
    		} else {
    			glBindBufferARB(GL_ARRAY_BUFFER_ARB, tcbo);
    			glTexCoordPointer(2, GL_FLOAT, 0, 0);
    			glBindBufferARB(GL_ARRAY_BUFFER_ARB, vbo);
    			glVertexPointer(3, GL_FLOAT, 0, 0);
    		}
		    Util.checkGLError();
		    
		    Visage.log.finest("Rendering");
		    if (tcbo == Integer.MAX_VALUE) {
		    	glDrawArrays(GL_QUADS, 0, vertices.length/5);
		    } else {
		    	glDrawArrays(GL_QUADS, 0, vertices.length/3);
		    }
			Util.checkGLError();
			
			/*glBegin(GL_QUADS);
			for (int i = 0; i < vertices.length/5; i++) {
				int idx = i*5;
				float x = vertices[idx];
				float y = vertices[idx+1];
				float z = vertices[idx+2];
				float u = vertices[idx+3];
				float v = vertices[idx+4];
				Visage.log.finest("Vertex "+x+", "+y+", "+z);
				Visage.log.finest("Texcoord "+u+", "+v);
				glTexCoord2f(u, v);
				glVertex3f(x, y, z);
			}
			glEnd();*/
		glPopMatrix();
	}
}
