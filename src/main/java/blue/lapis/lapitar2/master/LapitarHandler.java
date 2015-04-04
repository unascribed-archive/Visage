package blue.lapis.lapitar2.master;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.spacehq.mc.auth.GameProfile;
import org.spacehq.mc.auth.SessionService;
import org.spacehq.mc.auth.exception.ProfileException;

import blue.lapis.lapitar2.RenderMode;

public class LapitarHandler extends AbstractHandler {
	private final LapitarMaster master;
	public LapitarHandler(LapitarMaster master) {
		this.master = master;
	}
	private GameProfile profile;
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		if ("/test".equals(target)) {
			if (profile == null) {
				try {
					profile = new SessionService().fillProfileProperties(new GameProfile("93a09a98-1fbb-46da-85a6-f0bb1465dc53", "Aesen"));
				} catch (ProfileException e) {
					e.printStackTrace();
				}
			}
			byte[] png;
			try {
				png = master.fallback.draw(RenderMode.FACE, 2048, 2048, 4, profile);
			} catch (Exception e) {
				response.setContentType("text/plain");
				e.printStackTrace(response.getWriter());
				response.setStatus(500);
				response.flushBuffer();
				return;
			}
			response.setContentType("image/png");
			response.setContentLength(png.length);
			response.getOutputStream().write(png);
			response.getOutputStream().flush();
			response.setStatus(200);
			response.flushBuffer();
		}
	}
}
