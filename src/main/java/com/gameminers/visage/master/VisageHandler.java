package com.gameminers.visage.master;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import joptsimple.internal.Strings;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.spacehq.mc.auth.GameProfile;
import org.spacehq.mc.auth.GameProfileRepository;
import org.spacehq.mc.auth.ProfileLookupCallback;
import org.spacehq.mc.auth.SessionService;
import org.spacehq.mc.auth.exception.ProfileNotFoundException;

import com.gameminers.visage.Visage;
import com.gameminers.visage.RenderMode;
import com.gameminers.visage.master.exception.NoSlavesAvailableException;
import com.gameminers.visage.master.exception.RenderFailedException;

public class VisageHandler extends AbstractHandler {
	public static final String TYPES = "(face|head|portrait|player|skin)";
	
	public static final Pattern URL_PATTERN = Pattern.compile("/(.*)");
	public static final Pattern URL_WITH_MODE_PATTERN = Pattern.compile("/"+TYPES+"/(.*)");
	public static final Pattern URL_WITH_SIZE_AND_MODE_PATTERN = Pattern.compile("/"+TYPES+"/([0-9]+)/(.*)");
	
	public static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{1,16}$");
	public static final Pattern DASHLESS_UUID_PATTERN = Pattern.compile("^([A-Fa-f0-9]{8})([A-Fa-f0-9]{4})([A-Fa-f0-9]{4})([A-Fa-f0-9]{4})([A-Fa-f0-9]{12})$");
	public static final Pattern UUID_PATTERN = Pattern.compile("^[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}$");
	
	private final VisageMaster master;
	private final SessionService ss = new SessionService();
	private final GameProfileRepository gpr = new GameProfileRepository();
	
	private final boolean cacheHeader, slaveHeader, reportExceptions, usernames;
	private final int supersampling, minSize, defaultSize, maxSize;
	private final EnumSet<RenderMode> allowedModes = EnumSet.noneOf(RenderMode.class);
	private final String allowedModesS;
	
	public VisageHandler(VisageMaster master) {
		this.master = master;
		List<String> debug = master.config.getStringList("debug");
		slaveHeader = debug.contains("slave");
		cacheHeader = debug.contains("cache");
		reportExceptions = debug.contains("error");
		if (slaveHeader || cacheHeader) {
			Visage.log.warning("Visage is set to include debugging information in HTTP headers. This should be disabled in production.");
		}
		if (reportExceptions) {
			Visage.log.warning("Visage is set to include exception stack traces in failed requests. This can expose internal system information such as authentication information.");
		}
		usernames = master.config.getBoolean("lookup-names");
		supersampling = master.config.getInt("render.supersampling");
		minSize = master.config.getInt("render.min-size");
		defaultSize = master.config.getInt("render.default-size");
		maxSize = master.config.getInt("render.max-size");
		List<String> modes = master.config.getStringList("modes");
		for (String s : modes) {
			try {
				allowedModes.add(RenderMode.valueOf(s.toUpperCase()));
			} catch (IllegalArgumentException ignore) {}
		}
		allowedModesS = Strings.join(modes, ", ");
	}
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		if (!"GET".equals(request.getMethod())) {
			response.sendError(405);
			return;
		}
		int width = defaultSize;
		int height = width;
		RenderMode mode = RenderMode.PLAYER;
		String subject;
		
		// XXX Regex is probably a slow (and somewhat confusing) way to do this
		Matcher uwsam = URL_WITH_SIZE_AND_MODE_PATTERN.matcher(target);
		if (uwsam.matches()) {
			try {
				mode = RenderMode.valueOf(uwsam.group(1).toUpperCase());
			} catch (IllegalArgumentException e) {
				response.sendError(400, "Invalid render mode '"+uwsam.group(1)+"' - must be one of "+allowedModesS);
				return;
			}
			try {
				width = Math.max(minSize, Math.min(Integer.parseInt(uwsam.group(2)), maxSize));
			} catch (NumberFormatException e) {
				response.sendError(400, "Invalid size '"+uwsam.group(2)+"' - must be a decimal integer");
				return;
			}
			subject = uwsam.group(3);
		} else {
			Matcher uwm = URL_WITH_MODE_PATTERN.matcher(target);
			if (uwm.matches()) {
				try {
					mode = RenderMode.valueOf(uwm.group(1).toUpperCase());
				} catch (IllegalArgumentException e) {
					response.sendError(400, "Invalid render mode '"+uwm.group(1)+"' - must be one of "+allowedModesS);
					return;
				}
				subject = uwm.group(2);
			} else {
				Matcher u = URL_PATTERN.matcher(target);
				if (u.matches()) {
					subject = u.group(1);
				} else {
					response.sendError(404);
					return;
				}
			}
		}
		
		UUID uuid = null;
		Matcher dashed = UUID_PATTERN.matcher(subject);
		if (dashed.matches()) {
			uuid = UUID.fromString(subject);
		} else {
			Matcher dashless = DASHLESS_UUID_PATTERN.matcher(subject);
			if (dashless.matches()) {
				uuid = UUID.fromString(dashless.replaceAll("$1-$2-$3-$4-$5"));
			} else {
				if (usernames) {
					Matcher username = USERNAME_PATTERN.matcher(subject);
					if (username.matches()) {
						if (master.cache.hasUsername(subject)) {
							uuid = master.cache.getUUID(subject);
						} else {
							final Object[] result = new Object[1];
							gpr.findProfilesByNames(new String[] {subject}, new ProfileLookupCallback() {
								
								@Override
								public void onProfileLookupSucceeded(GameProfile profile) {
									result[0] = profile.getId();
								}
								
								@Override
								public void onProfileLookupFailed(GameProfile profile, Exception e) {
									result[0] = e;
								}
							});
							if (result[0] == null || result[0] instanceof ProfileNotFoundException) {
								response.sendError(400, "Could not find a player named '"+subject+"'");
								return;
							} else if (result[0] instanceof Exception) {
								Exception e = (Exception) result[0];
								Visage.log.log(Level.WARNING, "An error occurred while looking up a player name", e);
								if (reportExceptions) {
									response.setContentType("text/plain");
									e.printStackTrace(response.getWriter());
									response.setStatus(500);
									response.flushBuffer();
								} else {
									response.sendError(500, "Could not render your request");
								}
								return;
							} else if (result[0] instanceof UUID) {
								uuid = (UUID) result[0];
							} else {
								response.sendError(500, "Could not render your request");
								return;
							}
						}
					} else {
						response.sendError(400, "Subject must be a dashless UUID, dashed UUID, or username");
						return;
					}
				} else {
					response.sendError(400, "Subject must be a dashless or dashed UUID");
					return;
				}
			}
		}

		if (uuid == null) {
			response.sendError(500, "Could not render your request");
		}
		
		width *= supersampling;
		height *= supersampling;
		if (mode == RenderMode.PLAYER) {
			height = (int)Math.ceil(width * 1.625f);
		}
		final UUID uuidF = uuid;
		GameProfile profile;
		try {
			profile = master.cache.getProfile(uuid, new Callable<GameProfile>() {
				
				@Override
				public GameProfile call() throws Exception {
					GameProfile prof = new GameProfile(uuidF, null);
					return ss.fillProfileProperties(prof);
				}
			});
		} catch (ExecutionException e) {
			Visage.log.log(Level.WARNING, "An error occurred while resolving texture data", e);
			if (reportExceptions) {
				response.setContentType("text/plain");
				e.printStackTrace(response.getWriter());
				response.setStatus(500);
				response.flushBuffer();
			} else {
				response.sendError(500, "Could not render your request");
			}
			return;
		}
		
		RenderResponse resp;
		try {
			resp = master.renderRpc(mode, width, height, supersampling, profile);
		} catch (RenderFailedException e) {
			Visage.log.log(Level.WARNING, "An error occurred while rendering a request", e);
			if (reportExceptions) {
				response.setContentType("text/plain");
				e.printStackTrace(response.getWriter());
				response.setStatus(500);
				response.flushBuffer();
			} else {
				response.sendError(500, "Could not render your request");
			}
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
