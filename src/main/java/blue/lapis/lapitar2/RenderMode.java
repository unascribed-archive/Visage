package blue.lapis.lapitar2;

import java.util.logging.Level;

import blue.lapis.lapitar2.slave.render.HeadRenderer;
import blue.lapis.lapitar2.slave.render.PlayerRenderer;
import blue.lapis.lapitar2.slave.render.PortraitRenderer;
import blue.lapis.lapitar2.slave.render.Renderer;

public enum RenderMode {
	FACE(null),
	HEAD(HeadRenderer.class),
	PORTRAIT(PortraitRenderer.class),
	PLAYER(PlayerRenderer.class),
	SKIN(null);
	private final Class<? extends Renderer> renderer;
	private RenderMode(Class<? extends Renderer> renderer) {
		this.renderer = renderer;
	}
	public Renderer newRenderer() {
		if (renderer == null) return null;
		try {
			return renderer.newInstance();
		} catch (Exception e) {
			Lapitar.log.log(Level.SEVERE, "Could not instanciate Renderer for "+this, e);
			return null;
		}
	}
}
