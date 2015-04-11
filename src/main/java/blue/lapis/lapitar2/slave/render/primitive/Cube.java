package blue.lapis.lapitar2.slave.render.primitive;

import blue.lapis.lapitar2.slave.render.Renderer;

public class Cube extends Primitive {

	@Override
	public void render(Renderer renderer) {
		doRender(renderer, renderer.vbo, Renderer.vertices, true);
	}
}

