package blue.lapis.lapitar2.slave.render;

import blue.lapis.lapitar2.Lapitar;
import blue.lapis.lapitar2.slave.render.primitive.Cube;
import blue.lapis.lapitar2.slave.render.primitive.Plane;


public class HeadRenderer extends Renderer {
	private Cube helm, head;
	private Plane shadow;
	@Override
	protected void initPrimitives() {
		Lapitar.log.info("initPrimitives");
		prims.clear();
		shadow = new Plane();
		shadow.x = 0f;
		shadow.y = 0.7f;
		shadow.z = -5.4f;
		shadow.rotX = -25;
		shadow.rotY = 45;
		shadow.scaleX = shadow.scaleZ = 1.3f;
		shadow.textured = false;
		shadow.lit = false;
		shadow.r = shadow.g = shadow.b = 0;
		shadow.a = 0.5f;
		addPrimitive(shadow);
		head = new Cube();
		head.x = 0;
		head.y = -0.3f;
		head.z = -5.05f;
		head.rotX = -25;
		head.rotY = 45;
		addPrimitive(head);
		helm = new Cube();
		helm.scaleX = helm.scaleY = helm.scaleZ = 1.1f;
		helm.x = 0;
		helm.y = -0.25f;
		helm.z = -5f;
		helm.rotX = -25;
		helm.rotY = 45;
		addPrimitive(helm);
	}
}
