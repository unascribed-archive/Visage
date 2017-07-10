/*
 * The MIT License
 *
 * Copyright (c) 2015-2017, William Thompson (unascribed)
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

package com.surgeplay.visage.distributor;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import joptsimple.internal.Strings;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import redis.clients.jedis.Jedis;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.data.GameProfile.Texture;
import com.github.steveice10.mc.auth.data.GameProfile.TextureType;
import com.github.steveice10.mc.auth.exception.profile.ProfileNotFoundException;
import com.github.steveice10.mc.auth.service.ProfileService;
import com.github.steveice10.mc.auth.service.ProfileService.ProfileLookupCallback;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.auth.util.UUIDSerializer;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.surgeplay.visage.RenderMode;
import com.surgeplay.visage.Visage;
import com.surgeplay.visage.util.Profiles;

public class VisageHandler extends AbstractHandler {
	public static final Pattern URL_WITH_MODE_PATTERN = Pattern.compile("^/([A-Za-z]*?)/([A-Za-z0-9_]*|X-Steve|X-Alex)(?:\\.png)?$");
	public static final Pattern URL_WITH_SIZE_AND_MODE_PATTERN = Pattern.compile("^/([A-Za-z]*?)/([0-9]+)/([A-Za-z0-9_]*|X-Steve|X-Alex)(?:\\.png)?$");
	
	public static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{1,16}$");
	public static final Pattern DASHLESS_UUID_PATTERN = Pattern.compile("^([A-Fa-f0-9]{8})([A-Fa-f0-9]{4})([A-Fa-f0-9]{4})([A-Fa-f0-9]{4})([A-Fa-f0-9]{12})$");
	
	private final VisageDistributor distributor;
	private final SessionService ss = new SessionService();
	private final ProfileService gpr = new ProfileService();
	private final Gson gson = new GsonBuilder()
								.registerTypeAdapter(UUID.class, new UUIDSerializer())
								.create();
	
	private final boolean cacheHeader, rendererHeader, reportExceptions, usernames;
	private final int minSize, defaultSize, maxSize, maxAttempts, granularity;
	private final long resolverTtlMillis, skinTtlMillis;
	private final String baseUrl;
	private final EnumSet<RenderMode> allowedModes = EnumSet.noneOf(RenderMode.class);
	private final String allowedModesS;
	
	public VisageHandler(VisageDistributor distributor) {
		this.distributor = distributor;
		List<String> debug = distributor.config.getStringList("debug");
		rendererHeader = debug.contains("renderer");
		cacheHeader = debug.contains("cache");
		reportExceptions = debug.contains("error");
		if (rendererHeader || cacheHeader) {
			Visage.log.warning("Visage is set to include debugging information in HTTP headers. This should be disabled in production.");
		}
		if (reportExceptions) {
			Visage.log.warning("Visage is set to include exception stack traces in failed requests. This can expose internal system information such as authentication information.");
		}
		usernames = distributor.config.getBoolean("lookup-names");
		minSize = distributor.config.getInt("render.min-size");
		defaultSize = distributor.config.getInt("render.default-size");
		maxSize = distributor.config.getInt("render.max-size");
		maxAttempts = distributor.config.getInt("render.tries");
		granularity = distributor.config.getInt("render.size-granularity");
		resolverTtlMillis = distributor.config.getDuration("redis.resolver-ttl", TimeUnit.MILLISECONDS);
		skinTtlMillis = distributor.config.getDuration("redis.skin-ttl", TimeUnit.MILLISECONDS);
		baseUrl = distributor.config.getString("base-url");
		List<String> modes = distributor.config.getStringList("modes");
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
		final List<String> missed = cacheHeader ? new ArrayList<>() : null;
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
			} else if ("FULL".equalsIgnoreCase(modeStr) || "FRONTFULL".equalsIgnoreCase(modeStr)) {
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
		if (mode == RenderMode.FULL || mode == RenderMode.FRONTFULL) {
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
			String query = "";
			if (request.getQueryString() != null) {
				query = "?"+request.getQueryString();
			}
			sendPermanentRedirect(baseUrl+"/"+modeStr+"/"+rounded+"/"+subject+query, response);
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
						try (Jedis j = distributor.getResolverJedis();) {
							String resp = j.get(subject);
							if (resp != null) {
								response.sendRedirect(baseUrl+"/"+modeStr+"/"+height+"/"+resp.replace("-", ""));
								return;
							} else {
								if (cacheHeader) missed.add("username");
								final Object[] result = new Object[1];
								gpr.findProfilesByName(new String[] {subject}, new ProfileLookupCallback() {
									
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
									String query = "";
									if (request.getQueryString() != null) {
										query = "&"+request.getQueryString();
									}
									response.sendRedirect(baseUrl+"/"+modeStr+"/"+height+"/"+uuid.toString().replace("-", "")+"?resolvedUsername"+query);
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
		
		GameProfile profile = new GameProfile(uuid, "<unknown>");
		byte[] skin;
		try (Jedis sj = distributor.getSkinJedis()) {
			byte[] resp = sj.get((uuid.toString()+":profile").getBytes(Charsets.UTF_8));
			byte[] skinResp = sj.get((uuid.toString()+":skin").getBytes(Charsets.UTF_8));
			if (resp != null) {
				profile = ss.fillProfileTextures(Profiles.readGameProfile(new DataInputStream(new ByteArrayInputStream(resp))), false);
			} else {
				if (uuid.version() == 8) {
					profile = new GameProfile(uuid, subject.substring(2));
				} else {
					if (cacheHeader) missed.add("profile");
					profile = ss.fillProfileProperties(profile);
					profile = ss.fillProfileTextures(profile, false);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(baos);
					Profiles.writeGameProfile(dos, profile);
					dos.close();
					sj.set((uuid.toString()+":profile").getBytes(Charsets.UTF_8), baos.toByteArray());
					sj.pexpire(uuid.toString()+":profile", skinTtlMillis);
				}
			}
			if (skinResp != null && skinResp.length > 3) {
				skin = skinResp;
			} else {
				if (cacheHeader) missed.add("skin");
				Map<TextureType, Texture> tex = profile.getTextures();
				if (tex.containsKey(TextureType.SKIN)) {
					Texture skinTex = tex.get(TextureType.SKIN);
					try (InputStream in = URI.create(skinTex.getURL()).toURL().openStream()) {
						skin = ByteStreams.toByteArray(in);
					}
				} else {
					if (Profiles.isSlim(profile)) {
						skin = distributor.alex;
					} else {
						skin = distributor.steve;
					}
				}
				// If the skin starts with FF D8 and ends with FF D9...
				if ((skin[0]&0xFF) == 0xFF && (skin[1]&0xFF) == 0xD8 &&
						(skin[skin.length-2]&0xFF) == 0xFF && (skin[skin.length-2]&0xFF) == 0xD9) {
					// ...then it must be a JPEG. Some legacy skins are JPEGs.
					// Convert it to PNG for the renderer.
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ImageIO.write(ImageIO.read(new ByteArrayInputStream(skin)), "PNG", baos);
					skin = baos.toByteArray();
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
					skin = distributor.alex;
				} else {
					skin = distributor.steve;
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
				resp = distributor.renderRpc(mode, width, height, profile, skin, request.getParameterMap());
			} catch (Exception e) {
				ex = e;
				continue;
			}
			if (resp == null) {
				continue;
			}
			byte[] png = resp.png;
			if (request.getParameter("resolvedUsername") != null) {
				BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
				Graphics2D g = img.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g.setColor(Color.RED);
				g.fillRect(0, 0, img.getWidth(), 8);
				g.fillRect(0, 0, 8, img.getHeight());
				g.fillRect(img.getWidth()-8, 0, 8, img.getHeight());
				g.fillRect(0, img.getHeight()-8, img.getWidth(), 8);
				if (img.getWidth() >= 480) {
					g.setFont(Font.decode("Dialog-Bold").deriveFont(48f));
					g.drawString("USERNAME RENDER", 12, 56);
				} else if (img.getWidth() >= 256) {
					g.setFont(Font.decode("Dialog-Bold").deriveFont(24f));
					g.drawString("USERNAME RENDER", 12, 32);
				} else if (img.getWidth() >= 144) {
					g.setFont(Font.decode("Dialog-Bold").deriveFont(12f));
					g.drawString("USERNAME RENDER", 12, 24);
				} else if (img.getWidth() >= 96) {
					g.setFont(Font.decode("Dialog-Bold").deriveFont(12f));
					g.drawString("USERNAME", 12, 24);
					g.drawString("RENDER", 12, 38);
				} else if (img.getWidth() >= 64) {
					g.setFont(Font.decode("Dialog").deriveFont(8f));
					g.drawString("USERNAME", 10, 18);
					g.drawString("RENDER", 10, 26);
				}
				g.dispose();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(img, "PNG", baos);
				png = baos.toByteArray();
			}
			write(response, missed, png, resp.renderer);
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
	private void write(HttpServletResponse response, List<String> missed, byte[] png, String renderer) throws IOException {
		if (rendererHeader) {
			response.setHeader("X-Visage-Renderer", renderer);
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
