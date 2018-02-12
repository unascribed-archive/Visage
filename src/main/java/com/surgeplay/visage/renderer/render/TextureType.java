/*
 * The MIT License
 *
 * Copyright (c) 2015-2018, Una Thompson (unascribed)
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

package com.surgeplay.visage.renderer.render;

public enum TextureType {
	NONE,
	ALL(
		0, 0, 64, 64
	),
	HEAD(
		// Front (Red)
		8, 8, 8, 8,
		// Back (Blue)
		24, 8, 8, 8,
		// Top (Purple)
		8, 0, 8, 8,
		// Bottom (Gray)
		16, 0, 8, 8,
		// Left (Yellow)
		16, 8, 8, 8,
		// Right (Green)
		0, 8, 8, 8
	),
	HEAD2(
		// Front (Red)
		40, 8, 8, 8,
		// Back (Blue)
		56, 8, 8, 8,
		// Top (Purple)
		40, 0, 8, 8,
		// Bottom (Gray)
		48, 0, 8, 8,
		// Left (Yellow)
		48, 8, 8, 8,
		// Right (Green)
		32, 8, 8, 8
	),
	BODY(
		// Front (Red)
		20, 20, 8, 12,
		// Back (Blue)
		32, 20, 8, 12,
		// Top (Purple)
		20, 16, 8, 4,
		// Bottom (Gray)
		28, 16, 8, 4,
		// Left (Yellow)
		28, 20, 4, 12,
		// Right (Green)
		16, 20, 4, 12
	),
	BODY2(
		// Front (Red)
		20, 36, 8, 12,
		// Back (Blue)
		32, 36, 8, 12,
		// Top (Purple)
		20, 32, 8, 4,
		// Bottom (Gray)
		28, 32, 8, 4,
		// Left (Yellow)
		28, 36, 4, 12,
		// Right (Green)
		16, 36, 4, 12
	),
	RLEG(
		// Front (Red)
		4, 20, 4, 12,
		// Back (Blue)
		12, 20, 4, 12,
		// Top (Purple)
		4, 16, 4, 4,
		// Bottom (Gray)
		8, 16, 4, 4,
		// Left (Yellow)
		8, 20, 4, 12,
		// Right (Green)
		0, 20, 4, 12
	),
	RLEG2(
		// Front (Red)
		4, 36, 4, 12,
		// Back (Blue)
		12, 36, 4, 12,
		// Top (Purple)
		4, 32, 4, 4,
		// Bottom (Gray)
		8, 32, 4, 4,
		// Left (Yellow)
		8, 36, 4, 12,
		// Right (Green)
		0, 36, 4, 12
	),
	LLEG(
		// Front (Red)
		20, 52, 4, 12,
		// Back (Blue)
		28, 52, 4, 12,
		// Top (Purple)
		20, 48, 4, 4,
		// Bottom (Gray)
		24, 48, 4, 4,
		// Left (Yellow)
		24, 52, 4, 12,
		// Right (Green)
		16, 52, 4, 12
	),
	LLEG2(
		// Front (Red)
		4, 52, 4, 12,
		// Back (Blue)
		12, 52, 4, 12,
		// Top (Purple)
		4, 48, 4, 4,
		// Bottom (Gray)
		8, 48, 4, 4,
		// Left (Yellow)
		8, 52, 4, 12,
		// Right (Green)
		0, 52, 4, 12
	),
	RARM(
		// Front (Red)
		44, 20, 4, 12,
		// Back (Blue)
		52, 20, 4, 12,
		// Top (Purple)
		44, 16, 4, 4,
		// Bottom (Gray)
		48, 16, 4, 4,
		// Left (Yellow)
		48, 20, 4, 12,
		// Right (Green)
		40, 20, 4, 12
	),
	RARM_SLIM(
		// Front (Red)
		45, 20, 3, 12,
		// Back (Blue)
		53, 20, 3, 12,
		// Top (Purple)
		44, 16, 3, 4,
		// Bottom (Gray)
		49, 16, 3, 4,
		// Left (Yellow)
		49, 20, 4, 12,
		// Right (Green)
		40, 20, 4, 12
	),
	RARM2(
		// Front (Red)
		44, 36, 4, 12,
		// Back (Blue)
		52, 36, 4, 12,
		// Top (Purple)
		44, 32, 4, 4,
		// Bottom (Gray)
		48, 32, 4, 4,
		// Left (Yellow)
		48, 36, 4, 12,
		// Right (Green)
		40, 36, 4, 12
	),
	RARM2_SLIM(
		// Front (Red)
		45, 36, 3, 12,
		// Back (Blue)
		53, 36, 3, 12,
		// Top (Purple)
		44, 32, 3, 4,
		// Bottom (Gray)
		49, 32, 3, 4,
		// Left (Yellow)
		49, 36, 4, 12,
		// Right (Green)
		40, 36, 4, 12
	),
	LARM(
		// Front (Red)
		36, 52, 4, 12,
		// Back (Blue)
		44, 52, 4, 12,
		// Top (Purple)
		36, 48, 4, 4,
		// Bottom (Gray)
		40, 48, 4, 4,
		// Left (Yellow)
		40, 52, 4, 12,
		// Right (Green)
		32, 52, 4, 12
	),
	LARM_SLIM(
		// Front (Red)
		36, 52, 3, 12,
		// Back (Blue)
		44, 52, 3, 12,
		// Top (Purple)
		36, 48, 3, 4,
		// Bottom (Gray)
		40, 48, 3, 4,
		// Left (Yellow)
		40, 52, 4, 12,
		// Right (Green)
		32, 52, 4, 12
	),
	LARM2(
		// Front (Red)
		52, 52, 4, 12,
		// Back (Blue)
		60, 52, 4, 12,
		// Top (Purple)
		52, 48, 4, 4,
		// Bottom (Gray)
		56, 48, 4, 4,
		// Left (Yellow)
		58, 52, 4, 12,
		// Right (Green)
		48, 52, 4, 12
	),
	LARM2_SLIM(
		// Front (Red)
		52, 52, 3, 12,
		// Back (Blue)
		60, 52, 3, 12,
		// Top (Purple)
		52, 48, 3, 4,
		// Bottom (Gray)
		56, 48, 3, 4,
		// Left (Yellow)
		56, 52, 4, 12,
		// Right (Green)
		48, 52, 4, 12
	),
	
	HEAD_FRONT(
		8, 8, 8, 8
	),
	HEAD2_FRONT(
		40, 8, 8, 8
	),
	BODY_FRONT(
		20, 20, 8, 12
	),
	BODY2_FRONT(
		20, 36, 8, 12
	),
	RLEG_FRONT(
		4, 20, 4, 12
	),
	RLEG2_FRONT(
		4, 36, 4, 12
	),
	LLEG_FRONT(
		20, 52, 4, 12
	),
	LLEG2_FRONT(
		4, 52, 4, 12
	),
	RARM_FRONT(
		44, 20, 4, 12
	),
	RARM_SLIM_FRONT(
		45, 20, 3, 12
	),
	RARM2_FRONT(
		44, 36, 4, 12
	),
	RARM2_SLIM_FRONT(
		45, 36, 3, 12
	),
	LARM_FRONT(
		36, 52, 4, 12
	),
	LARM_SLIM_FRONT(
		36, 52, 3, 12
	),
	LARM2_FRONT(
		52, 52, 4, 12
	),
	LARM2_SLIM_FRONT(
		52, 52, 3, 12
	),
	;
	
	public final float[] u = new float[24];
	public final float[] v = new float[24];
	
	private TextureType() {}
	
	private static final int tex_w = 64;
	private static final int tex_h = 64;
	
	// constructor uses varargs for compactness
	// arguments are effectively:
	// <side>_x, <side>_y, <side>_width, <side>_height
	// where <side> is the face in question, in order:
	// Front, Back, Top, Bottom, Left, Right
	private TextureType(int... assorted) {
		for (int i = 0; i < assorted.length/4; i++) {
			int idx = i*4;
			
			int x = assorted[idx];
			int y = assorted[idx+1];
			int edgeX = x + assorted[idx+2];
			int edgeY = y + assorted[idx+3];
			
			// slightly shrink the box to prevent texture bleeding
			
			u[idx  ] = div(tex_w,     x)+0.001f;
			v[idx  ] = div(tex_h, edgeY)-0.001f;
			
			u[idx+1] = div(tex_w, edgeX)-0.001f;
			v[idx+1] = div(tex_h, edgeY)-0.001f;
			
			u[idx+2] = div(tex_w, edgeX)-0.001f;
			v[idx+2] = div(tex_h,     y)+0.001f;
			
			u[idx+3] = div(tex_w,     x)+0.001f;
			v[idx+3] = div(tex_h,     y)+0.001f;
		}
	}

	private float div(float max, float x) { // upcasting!
		return x / max;
	}

}
