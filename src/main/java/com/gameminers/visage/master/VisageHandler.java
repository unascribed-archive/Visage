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
import com.google.common.collect.Lists;

public class VisageHandler extends AbstractHandler {
	public static final Pattern URL_WITH_MODE_PATTERN = Pattern.compile("^/([A-Za-z]*?)/([A-Za-z0-9_]*|X-Steve|X-Alex)(?:\\.png)?$");
	public static final Pattern URL_WITH_SIZE_AND_MODE_PATTERN = Pattern.compile("^/([A-Za-z]*?)/([0-9]+)/([A-Za-z0-9_]*|X-Steve|X-Alex)(?:\\.png)?$");
	
	public static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{1,16}$");
	public static final Pattern DASHLESS_UUID_PATTERN = Pattern.compile("^([A-Fa-f0-9]{8})([A-Fa-f0-9]{4})([A-Fa-f0-9]{4})([A-Fa-f0-9]{4})([A-Fa-f0-9]{12})$");
	public static final Pattern UUID_PATTERN = Pattern.compile("^[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}$");
	
	private final VisageMaster master;
	private final SessionService ss = new SessionService();
	private final GameProfileRepository gpr = new GameProfileRepository();
	
	private final boolean cacheHeader, slaveHeader, reportExceptions, usernames;
	private final int supersampling, minSize, defaultSize, maxSize, maxAttempts;
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
		maxAttempts = master.config.getInt("render.tries");
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
		RenderMode mode = RenderMode.PLAYER;
		String subject;
		final List<String> missed = Lists.newArrayList("skin", "render"); // TODO
		
		// XXX Regex is probably a slow (and somewhat confusing) way to do this
		Matcher uwsam = URL_WITH_SIZE_AND_MODE_PATTERN.matcher(target);
		if (uwsam.matches()) {
			try {
				mode = RenderMode.valueOf(uwsam.group(1).toUpperCase());
				if (!allowedModes.contains(mode)) {
					throw new IllegalArgumentException();
				}
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
					if (!allowedModes.contains(mode)) {
						throw new IllegalArgumentException();
					}
				} catch (IllegalArgumentException e) {
					response.sendError(400, "Invalid render mode '"+uwm.group(1)+"' - must be one of "+allowedModesS);
					return;
				}
				subject = uwm.group(2);
			} else {
				response.sendError(404);
				return;
			}
		}
		
		UUID uuid = null;
		if (subject.equals("X-Steve")) {
			uuid = new UUID(0 | (8 << 12), 0);
		} else if (subject.equals("X-Alex")) {
			uuid = new UUID(0 | (8 << 12), 1);
		} else {
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
		}

		if (uuid == null) {
			response.sendError(500, "Could not render your request");
		}
		
		int height = width;
		
		width *= supersampling;
		height *= supersampling;
		if (mode == RenderMode.PLAYER) {
			width = (int)Math.ceil(width * 0.625f);
		}
		final UUID uuidF = uuid;
		final String subjectF = subject;
		GameProfile profile;
		try {
			profile = master.cache.getProfile(uuid, new Callable<GameProfile>() {
				
				@Override
				public GameProfile call() throws Exception {
					if (uuidF.version() == 8) {
						return new GameProfile(uuidF, subjectF.substring(2));
					}
					missed.add("profile");
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
		
		RenderResponse resp = null;
		Exception ex = null;
		int attempts = 0;
		while (attempts < maxAttempts) {
			attempts++;
			try {
				resp = master.renderRpc(mode, width, height, supersampling, profile, request.getParameterMap());
			} catch (Exception e) {
				ex = e;
				continue;
			}
			if (resp == null) {
				continue;
			}
			if (slaveHeader) {
				response.setHeader("X-Visage-Slave", resp.slave);
			}
			response.setContentType("image/png");
			response.setContentLength(resp.png.length);
			if (cacheHeader) {
				response.setHeader("X-Visage-Cache-Miss", Strings.join(missed, ", "));
			}
			response.getOutputStream().write(resp.png);
			response.getOutputStream().flush();
			response.setStatus(200);
			response.flushBuffer();
			return;
		}
		if (ex != null) {
			Visage.log.log(Level.WARNING, "An error occurred while rendering a request", ex);
			if (reportExceptions) {
				response.setContentType("text/plain");
				ex.printStackTrace(response.getWriter());
				response.setStatus(500);
				response.flushBuffer();
			} else {
				response.sendError(500, "Could not render your request");
			}
			return;
		} else if (resp == null) {
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("Could not render your request");
			response.setStatus(500);
			response.flushBuffer();
		}
	}
}
