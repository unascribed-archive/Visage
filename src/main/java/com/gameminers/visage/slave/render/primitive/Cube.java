package com.gameminers.visage.slave.render.primitive;

import com.gameminers.visage.slave.render.Renderer;

public class Cube extends Primitive {

	@Override
	public void render(Renderer renderer) {
		doRender(renderer, renderer.vbo, Renderer.vertices);
	}
}

