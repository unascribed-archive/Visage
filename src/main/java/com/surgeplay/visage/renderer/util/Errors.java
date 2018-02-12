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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.surgeplay.visage.Visage;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GL45;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.glfw.GLFW.*;

public class Errors {
	private static Multimap<Integer, String> mapping;
	private static void buildMapping() {
		if (mapping != null) return;
		Multimap<Integer, String> map = HashMultimap.create();
		List<Class<?>> classes = ImmutableList.of(
				GL11.class, GL12.class, GL13.class, GL14.class, GL15.class,
				GL20.class, GL21.class, GL30.class, GL31.class, GL32.class,
				GL33.class, GL40.class, GL41.class, GL42.class, GL43.class,
				GL44.class, GL45.class, GLFW.class
				);
		for (Class<?> clazz : classes) {
			for (Field f : clazz.getDeclaredFields()) {
				if (f.getName().toUpperCase(Locale.ROOT).equals(f.getName()) &&
						f.getType() == int.class && Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers())) {
					List<String> li = Splitter.on('_').splitToList(f.getName());
					li = li.subList(1, li.size());
					String clean =
						Joiner.on(' ').join(
							li.stream()
								.map(Errors::toTitleCase)
								.iterator());
					try {
						map.put(f.getInt(null), clean);
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}
		}
		mapping = map;
	}
	
	public static void main(String[] args) {
		buildMapping();
	}
	
	private static String toTitleCase(String str) {
		return str.charAt(0)+str.substring(1).toLowerCase(Locale.ROOT);
	}
	
	public static void checkGLError() {
		if (Visage.debug) {
			int err = glGetError();
			while (err != GL_NO_ERROR) {
				buildMapping();
				Visage.log.warning("== GL ERROR ==");
				Visage.log.warning("0x"+Integer.toHexString(err).toUpperCase(Locale.ROOT)+" ("+Joiner.on(", ").join(mapping.get(err))+")");
				for (StackTraceElement ste : new Throwable().fillInStackTrace().getStackTrace()) {
					Visage.log.warning(ste.toString());
				}
				err = glGetError();
			}
		}
	}
	
	public static void checkGLFWError() {
		if (Visage.debug) {
			int err = glfwGetError();
			while (err != GLFW_NO_ERROR) {
				buildMapping();
				Visage.log.warning("== GLFW ERROR ==");
				Visage.log.warning("0x"+Integer.toHexString(err).toUpperCase(Locale.ROOT)+" ("+Joiner.on(", ").join(mapping.get(err))+")");
				for (StackTraceElement ste : new Throwable().fillInStackTrace().getStackTrace()) {
					Visage.log.warning(ste.toString());
				}
				err = glfwGetError();
			}
		}
	}
	
	public static void checkFramebufferStatus() {
		if (Visage.debug) {
			int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
			buildMapping();
			if (status != GL_FRAMEBUFFER_COMPLETE) {
				Visage.log.warning("== FRAMEBUFFER INCOMPLETE ==");
				Visage.log.warning("0x"+Integer.toHexString(status).toUpperCase(Locale.ROOT)+" ("+Joiner.on(", ").join(mapping.get(status))+")");
				for (StackTraceElement ste : new Throwable().fillInStackTrace().getStackTrace()) {
					Visage.log.warning(ste.toString());
				}
			}
		}
	}
}
