package com.gameminers.visage.slave.render.primitive;

import com.gameminers.visage.slave.render.Renderer;

public class Plane extends Primitive {
	@Override
	public void render(Renderer renderer) {
		doRender(renderer, renderer.planeVbo, Integer.MAX_VALUE, Renderer.planeVertices);
	}
}
