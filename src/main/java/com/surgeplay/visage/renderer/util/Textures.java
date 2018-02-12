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

package com.surgeplay.visage.renderer.util;

import static com.surgeplay.visage.renderer.util.Errors.checkGLError;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

import com.surgeplay.visage.Visage;

public class Textures {

	public static void upload(BufferedImage img, int format, int tex) {
		int width = img.getWidth();
		int height = img.getHeight();
		if (Visage.trace) Visage.log.finest("Uploading "+width+"x"+height+" ("+(width*height)+" pixel) image");
		
		BufferedImage unindexed = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		
		int[] argb = new int[width*height];
		img.getRGB(0, 0, width, height, argb, 0, width);
		
		unindexed.setRGB(0, 0, width, height, argb, 0, width);
		unindexed.coerceData(true);
		unindexed.getRGB(0, 0, width, height, argb, 0, width);
		
		IntBuffer buf = BufferUtils.createIntBuffer(width*height);
		buf.put(argb);
		buf.flip();
		
		glBindTexture(GL_TEXTURE_2D, tex);
		glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, GL_BGRA, GL_UNSIGNED_BYTE, buf);
		
		checkGLError();
	}
	
}
