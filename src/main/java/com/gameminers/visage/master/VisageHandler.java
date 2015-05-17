/*
 * Visage
 * Copyright (c) 2015, Aesen Vismea <aesen@gameminers.com>
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.gameminers.visage.master;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
import org.spacehq.mc.auth.ProfileTexture;
import org.spacehq.mc.auth.ProfileTextureType;
import org.spacehq.mc.auth.SessionService;
import org.spacehq.mc.auth.exception.ProfileNotFoundException;
import org.spacehq.mc.auth.properties.PropertyMap;
import org.spacehq.mc.auth.serialize.GameProfileSerializer;
import org.spacehq.mc.auth.serialize.PropertyMapSerializer;
import org.spacehq.mc.auth.serialize.UUIDSerializer;
import org.spacehq.mc.auth.util.URLUtils;

import redis.clients.jedis.Jedis;

import com.gameminers.visage.Visage;
import com.gameminers.visage.RenderMode;
import com.gameminers.visage.util.Profiles;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class VisageHandler extends AbstractHandler {
	public static final Pattern URL_WITH_MODE_PATTERN = Pattern.compile("^/([A-Za-z]*?)/([A-Za-z0-9_]*|X-Steve|X-Alex)(?:\\.png)?$");
	public static final Pattern URL_WITH_SIZE_AND_MODE_PATTERN = Pattern.compile("^/([A-Za-z]*?)/([0-9]+)/([A-Za-z0-9_]*|X-Steve|X-Alex)(?:\\.png)?$");
	
	public static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{1,16}$");
	public static final Pattern DASHLESS_UUID_PATTERN = Pattern.compile("^([A-Fa-f0-9]{8})([A-Fa-f0-9]{4})([A-Fa-f0-9]{4})([A-Fa-f0-9]{4})([A-Fa-f0-9]{12})$");
	
	private static final long ONE_DAY = 1000 * 60 * 60 * 24;
	private static final long THIRTY_DAYS = ONE_DAY * 30;
	
	private final VisageMaster master;
	private final SessionService ss = new SessionService();
	private final GameProfileRepository gpr = new GameProfileRepository();
	private final Gson gson = new GsonBuilder()
								.registerTypeAdapter(GameProfile.class, new GameProfileSerializer())
								.registerTypeAdapter(PropertyMap.class, new PropertyMapSerializer())
								.registerTypeAdapter(UUID.class, new UUIDSerializer())
								.create();
	
	private final boolean cacheHeader, slaveHeader, reportExceptions, usernames;
	private final int supersampling, minSize, defaultSize, maxSize, maxAttempts, granularity;
	private final long resolverTtlMillis, skinTtlMillis;
	private final String baseUrl;
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
		granularity = master.config.getInt("render.size-granularity");
		resolverTtlMillis = master.config.getDuration("redis.resolver-ttl", TimeUnit.MILLISECONDS);
		skinTtlMillis = master.config.getDuration("redis.skin-ttl", TimeUnit.MILLISECONDS);
		baseUrl = master.config.getString("base-url");
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
		baseRequest.setHandled(true);
		if (!"GET".equals(request.getMethod())) {
			response.sendError(405);
			return;
		}
		RenderMode mode = RenderMode.FULL;
		String subject;
		final List<String> missed = cacheHeader ? new ArrayList<String>() : null;
		if (target.contains("-") && !(target.endsWith("X-Steve") || target.endsWith("X-Alex"))) {
			sendPermanentRedirect(baseUrl+target.replace("-", ""), response);
			return;
		}
		// XXX Regex is probably a slow (and somewhat confusing) way to do this
		Matcher uwsam = URL_WITH_SIZE_AND_MODE_PATTERN.matcher(target);
		String modeStr;
		int size = defaultSize;
		if (uwsam.matches()) {
			try {
				modeStr = uwsam.group(1);
				if (!allowedModes.contains(mode)) {
					throw new IllegalArgumentException();
				}
			} catch (IllegalArgumentException e) {
				response.sendError(400, "Invalid render mode '"+uwsam.group(1)+"' - must be one of "+allowedModesS);
				return;
			}
			try {
				size = Integer.parseInt(uwsam.group(2));
			} catch (NumberFormatException e) {
				response.sendError(400, "Invalid size '"+uwsam.group(2)+"' - must be a decimal integer");
				return;
			}
			subject = uwsam.group(3);
		} else {
			Matcher uwm = URL_WITH_MODE_PATTERN.matcher(target);
			if (uwm.matches()) {
				modeStr = uwm.group(1);
				subject = uwm.group(2);
			} else {
				response.sendError(404);
				return;
			}
		}
		int width = size;
		int height = size;
		try {
			if ("PLAYER".equalsIgnoreCase(modeStr)) {
				height *= 1.625;
				sendPermanentRedirect(baseUrl+"/full/"+height+"/"+subject, response);
				return;
			} else if ("FULL".equalsIgnoreCase(modeStr)) {
				width = (int)Math.ceil(width / 1.625f);
			} else if ("HELM".equalsIgnoreCase(modeStr)) {
				sendPermanentRedirect(baseUrl+"/face/"+height+"/"+subject, response);
				return;
			} else if ("PORTRAIT".equalsIgnoreCase(modeStr)) {
				sendPermanentRedirect(baseUrl+"/bust/"+height+"/"+subject, response);
				return;
			}
			mode = RenderMode.valueOf(modeStr.toUpperCase());
			if (!allowedModes.contains(mode)) {
				throw new IllegalArgumentException();
			}
		} catch (IllegalArgumentException e) {
			response.sendError(400, "Invalid render mode '"+modeStr+"' - must be one of "+allowedModesS);
			return;
		}
		if (mode == RenderMode.FULL) {
			int clamped = Math.max(minSize, Math.min(height, (int)(maxSize*1.625)));
			if (clamped != height) {
				sendPermanentRedirect(baseUrl+"/"+modeStr+"/"+clamped+"/"+subject, response);
				return;
			}
		} else {
			int clamped = Math.max(minSize, Math.min(height, maxSize));
			if (clamped != height) {
				sendPermanentRedirect(baseUrl+"/"+modeStr+"/"+clamped+"/"+subject, response);
				return;
			}
		}
		int rounded = Math.round(height / (float) granularity)*granularity;
		if (rounded != height) {
			sendPermanentRedirect(baseUrl+"/"+modeStr+"/"+rounded+"/"+subject, response);
			return;
		}
		
		UUID uuid = null;
		if (subject.equals("X-Steve")) {
			uuid = new UUID(0 | (8 << 12), 0);
		} else if (subject.equals("X-Alex")) {
			uuid = new UUID(0 | (8 << 12), 1);
		} else {
			Matcher dashless = DASHLESS_UUID_PATTERN.matcher(subject);
			if (dashless.matches()) {
				uuid = UUID.fromString(dashless.replaceAll("$1-$2-$3-$4-$5"));
			} else {
				if (usernames) {
					Matcher username = USERNAME_PATTERN.matcher(subject);
					if (username.matches()) {
						try (Jedis j = master.getResolverJedis();) {
							String resp = j.get(subject);
							if (resp != null) {
								sendPermanentRedirect(baseUrl+"/"+modeStr+"/"+height+"/"+resp.replace("-", ""), response);
								return;
							} else {
								if (cacheHeader) missed.add("username");
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
									sendPermanentRedirect(baseUrl+"/"+modeStr+"/"+height+"/"+uuid.toString().replace("-", ""), response);
									j.set(subject, uuid.toString());
									j.pexpire(subject, resolverTtlMillis);
									return;
								} else {
									response.sendError(500, "Could not render your request");
									return;
								}
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
		
		GameProfile profile = new GameProfile(uuid, "<unknown>");
		byte[] skin;
		try (Jedis sj = master.getSkinJedis()) {
			String resp = sj.get(uuid.toString()+":profile");
			byte[] skinResp = sj.get((uuid.toString()+":skin").getBytes(Charsets.UTF_8));
			if (resp != null) {
				profile = gson.fromJson(resp, GameProfile.class);
			} else {
				if (uuid.version() == 8) {
					profile = new GameProfile(uuid, subject.substring(2));
				} else {
					if (cacheHeader) missed.add("profile");
					profile = ss.fillProfileProperties(profile);
					sj.set(uuid.toString()+":profile", gson.toJson(profile));
					sj.pexpire(uuid.toString()+":profile", skinTtlMillis);
				}
			}
			if (skinResp != null && skinResp.length > 3) {
				skin = skinResp;
			} else {
				if (cacheHeader) missed.add("skin");
				Map<ProfileTextureType, ProfileTexture> tex = ss.getTextures(profile, false);
				if (tex.containsKey(ProfileTextureType.SKIN)) {
					ProfileTexture skinTex = tex.get(ProfileTextureType.SKIN);
					try (InputStream in = URLUtils.constantURL(skinTex.getUrl()).openStream()) {
						skin = ByteStreams.toByteArray(in);
					}
				} else {
					if (Profiles.isSlim(profile)) {
						skin = master.alex;
					} else {
						skin = master.steve;
					}
				}
				sj.set((uuid.toString()+":skin").getBytes(Charsets.UTF_8), skin);
				sj.pexpire(uuid.toString()+":skin", skinTtlMillis);
			}
		} catch (Exception e) {
			Visage.log.log(Level.WARNING, "An error occurred while resolving texture data", e);
			if (reportExceptions) {
				response.setContentType("text/plain");
				e.printStackTrace(response.getWriter());
				response.setStatus(500);
				response.flushBuffer();
				return;
			} else {
				if (Profiles.isSlim(profile)) {
					skin = master.alex;
				} else {
					skin = master.steve;
				}
			}
		}
		
		if (mode == RenderMode.SKIN) {
			write(response, missed, skin, "none");
			return;
		}
		
		RenderResponse resp = null;
		Exception ex = null;
		int attempts = 0;
		while (attempts < maxAttempts) {
			attempts++;
			try {
				resp = master.renderRpc(mode, width, height, supersampling, profile, skin, request.getParameterMap());
			} catch (Exception e) {
				ex = e;
				continue;
			}
			if (resp == null) {
				continue;
			}
			write(response, missed, resp.png, resp.slave);
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
	private void sendPermanentRedirect(String path, HttpServletResponse response) {
		response.setStatus(301);
		response.setHeader("Location", path);
	}
	private void write(HttpServletResponse response, List<String> missed, byte[] png, String slave) throws IOException {
		if (slaveHeader) {
			response.setHeader("X-Visage-Slave", slave);
		}
		response.setContentType("image/png");
		response.setContentLength(png.length);
		if (cacheHeader) {
			if (missed.isEmpty()) {
				response.setHeader("X-Visage-Cache-Miss", "none");
			} else {
				response.setHeader("X-Visage-Cache-Miss", Strings.join(missed, ", "));
			}
		}
		response.getOutputStream().write(png);
		response.getOutputStream().flush();
		response.setStatus(200);
		response.flushBuffer();
	}
}
