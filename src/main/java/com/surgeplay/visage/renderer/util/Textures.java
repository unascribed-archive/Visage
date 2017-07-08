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
		int[] argb = new int[width*height];
		img.getRGB(0, 0, width, height, argb, 0, width);
		IntBuffer buf = BufferUtils.createIntBuffer(width*height);
		buf.put(argb);
		buf.flip();
		
		glBindTexture(GL_TEXTURE_2D, tex);
		glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, GL_BGRA, GL_UNSIGNED_BYTE, buf);
		
		checkGLError();
	}
	
}
