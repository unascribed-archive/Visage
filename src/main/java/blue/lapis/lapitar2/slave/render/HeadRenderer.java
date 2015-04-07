package blue.lapis.lapitar2.slave.render;

import blue.lapis.lapitar2.Lapitar;


public class HeadRenderer extends Renderer {
	private Cube helm, head;
	@Override
	protected void initCubes() {
		Lapitar.log.info("initCubes");
		cubes.clear();
		/*helm = new Cube();
		helm.scaleX = helm.scaleY = helm.scaleZ = 1.1f;
		helm.x = 0;
		helm.y = 0.25f;
		helm.z = -5f;
		helm.rotX = ((float)Math.random()*90f)-45f;
		helm.rotY = ((float)Math.random()*90f)-45f;
		addCube(helm);*/
		head = new Cube();
		head.x = 0;
		head.y = 0.3f;
		head.z = -5.05f;
		head.rotX = ((float)Math.random()*90f)-45f;
		head.rotY = ((float)Math.random()*90f)-45f;
		addCube(head);
	}
}
