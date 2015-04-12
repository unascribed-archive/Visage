package com.gameminers.visage;

import java.util.logging.Level;

import com.gameminers.visage.slave.render.HeadRenderer;
import com.gameminers.visage.slave.render.PlayerRenderer;
import com.gameminers.visage.slave.render.PortraitRenderer;
import com.gameminers.visage.slave.render.Renderer;

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
			Visage.log.log(Level.SEVERE, "Could not instanciate Renderer for "+this, e);
			return null;
		}
	}
}
