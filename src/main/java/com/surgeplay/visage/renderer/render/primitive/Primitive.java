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

package com.surgeplay.visage.renderer.render.primitive;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import com.surgeplay.visage.Visage;
import com.surgeplay.visage.renderer.render.Renderer;
import com.surgeplay.visage.renderer.render.TextureType;
import com.surgeplay.visage.renderer.util.Errors;

public abstract class Primitive {
	public float scaleX = 1.0f;
	public float scaleY = 1.0f;
	public float scaleZ = 1.0f;
	public float x, y, z, rotX, rotY, rotZ;
	public float anchorX, anchorY, anchorZ;
	public boolean lit = true;
	
	public boolean textured = true;
	public TextureType texture = TextureType.NONE;
	
	protected boolean inStage = true;
	
	public boolean depthMask = true;
	
	public abstract void render(Renderer renderer);
	
	protected void doRender(Renderer renderer, int vbo, int tcbo, float[] vertices) {
		glPushMatrix();
			glDepthMask(depthMask);
			if (Visage.trace) Visage.log.finest("Rendering "+getClass().getSimpleName());
			if (Visage.trace) Visage.log.finest("Translating to "+x+", "+y+", "+z);
			glTranslatef(x, y, z);
			if (Visage.trace) Visage.log.finest("Rotating by "+rotX+"°, "+rotY+"°, "+rotZ+"°, anchored at "+anchorX+", "+anchorY+", "+anchorZ);
			glTranslatef(anchorX, anchorY, anchorZ);
			glRotatef(rotX, 1, 0, 0);
			glRotatef(rotY, 0, 1, 0);
			glRotatef(rotZ, 0, 0, 1);
			glTranslatef(-anchorX, -anchorY, -anchorZ);
			if (Visage.trace) Visage.log.finest("Scaling by "+scaleX+"x, "+scaleY+"x, "+scaleZ+"x");
			glScalef(scaleX, scaleY*-1, scaleZ);
			
			if (!inStage && lit) {
				if (Visage.trace) Visage.log.finest("Enabling lighting");
				glEnable(GL_LIGHTING);
			} else if (!inStage) {
				if (Visage.trace) Visage.log.finest("Disabling lighting");
				glDisable(GL_LIGHTING);
			}
			if (textured) {
				if (Visage.trace) Visage.log.finest("Enabling texturing - texture "+texture);
				glEnable(GL_TEXTURE_2D);
				if (texture == TextureType.ALL) {
					glBindTexture(GL_TEXTURE_2D, renderer.owner.shadowTexture);
					glUseProgram(0);
				} else {
					glBindTexture(GL_TEXTURE_2D, renderer.owner.skinFboTex);
					glUseProgram(renderer.owner.textureFilterProgram);
				}
			} else {
				if (Visage.trace) Visage.log.finest("Disabling texturing");
				glDisable(GL_TEXTURE_2D);
				glUseProgram(0);
			}
			glEnable(GL_BLEND);
			glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
			glDisable(GL_ALPHA_TEST);
			Errors.checkGLError();
			
			if (Visage.trace) Visage.log.finest("Setting VBO");
			glEnableClientState(GL_VERTEX_ARRAY);
			glEnableClientState(GL_TEXTURE_COORD_ARRAY);
			if (tcbo == Integer.MAX_VALUE) {
				glBindBuffer(GL_ARRAY_BUFFER, vbo);
				glTexCoordPointer(2, GL_FLOAT, 20, 12);
				glVertexPointer(3, GL_FLOAT, 20, 0);
			} else {
				glEnableClientState(GL_NORMAL_ARRAY);
				glBindBuffer(GL_ARRAY_BUFFER, tcbo);
				glTexCoordPointer(2, GL_FLOAT, 0, 0);
				glBindBuffer(GL_ARRAY_BUFFER, vbo);
				glVertexPointer(3, GL_FLOAT, 24, 0);
				glNormalPointer(GL_FLOAT, 24, 12);
			}
			Errors.checkGLError();
			
			if (Visage.trace) Visage.log.finest("Rendering");
			if (tcbo == Integer.MAX_VALUE) {
				glDrawArrays(GL_QUADS, 0, vertices.length/5);
			} else {
				glDrawArrays(GL_QUADS, 0, vertices.length/6);
			}
			Errors.checkGLError();
			
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
			glDepthMask(true);
		glPopMatrix();
	}
}
