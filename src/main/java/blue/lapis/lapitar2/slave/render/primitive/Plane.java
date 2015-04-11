package blue.lapis.lapitar2.slave.render.primitive;

import blue.lapis.lapitar2.slave.render.Renderer;

public class Plane extends Primitive {
	@Override
	public void render(Renderer renderer) {
		doRender(renderer, renderer.planeVbo, Renderer.planeVertices, false);
	}
}
