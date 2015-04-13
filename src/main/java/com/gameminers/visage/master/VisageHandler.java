package com.gameminers.visage.master;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.spacehq.mc.auth.GameProfile;
import org.spacehq.mc.auth.SessionService;
import org.spacehq.mc.auth.exception.ProfileException;

import com.gameminers.visage.Visage;
import com.gameminers.visage.RenderMode;
import com.gameminers.visage.master.exception.NoSlavesAvailableException;
import com.gameminers.visage.master.exception.RenderFailedException;

public class VisageHandler extends AbstractHandler {
	private final VisageMaster master;
	private final boolean slaveHeader, reportExceptions;
	public VisageHandler(VisageMaster master) {
		this.master = master;
		List<String> debug = master.config.getStringList("debug");
		slaveHeader = debug.contains("slave");
		reportExceptions = debug.contains("error");
		try {
			profile = new SessionService().fillProfileProperties(new GameProfile("93a09a98-1fbb-46da-85a6-f0bb1465dc53", "Aesen"));
		} catch (ProfileException e) {
			e.printStackTrace();
		}
	}
	private GameProfile profile;
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		if ("/test".equals(target)) {
			if (!"GET".equals(request.getMethod())) {
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("Unsupported method");
				response.setStatus(405);
				response.flushBuffer();
				return;
			}
			RenderResponse resp;
			try {
				resp = master.renderRpc(RenderMode.valueOf(request.getQueryString() == null ? "HEAD" : request.getQueryString()), 2048, 2048, 4, profile);
			} catch (RenderFailedException e) {
				Visage.log.log(Level.WARNING, "An error occurred while rendering a request", e);
				response.setContentType("text/plain");
				if (reportExceptions) {
					e.printStackTrace(response.getWriter());
				} else {
					response.getWriter().println("Could not render your request");
				}
				response.setStatus(500);
				response.flushBuffer();
				return;
			} catch (NoSlavesAvailableException e) {
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("No slaves are available to render your request");
				response.setStatus(503);
				response.flushBuffer();
				return;
			}
			if (resp == null) {
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("Could not render your request");
				response.setStatus(500);
				response.flushBuffer();
				return;
			}
			if (slaveHeader) {
				response.setHeader("X-Visage-Slave", resp.slave);
			}
			response.setContentType("image/png");
			response.setContentLength(resp.png.length);
			response.getOutputStream().write(resp.png);
			response.getOutputStream().flush();
			response.setStatus(200);
			response.flushBuffer();
		}
	}
}
