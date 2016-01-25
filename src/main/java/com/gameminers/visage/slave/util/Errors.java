package com.gameminers.visage.slave.util;

import org.lwjgl.opengl.Util;

import com.gameminers.visage.slave.VisageSlave;

public class Errors {
	public static void checkGLError() {
		if (VisageSlave.explodeOnError) {
			Util.checkGLError();
		}
	}
}
