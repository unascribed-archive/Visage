/* This file is part of MCAuthLib.
 * Copyright (C) 2013-2014 Steveice10
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.spacehq.mc.auth;

import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.spacehq.mc.auth.exception.AuthenticationException;
import org.spacehq.mc.auth.exception.AuthenticationUnavailableException;
import org.spacehq.mc.auth.exception.ProfileException;
import org.spacehq.mc.auth.exception.ProfileLookupException;
import org.spacehq.mc.auth.exception.ProfileNotFoundException;
import org.spacehq.mc.auth.exception.ProfileTextureException;
import org.spacehq.mc.auth.exception.PropertyException;
import org.spacehq.mc.auth.properties.Property;
import org.spacehq.mc.auth.request.JoinServerRequest;
import org.spacehq.mc.auth.response.HasJoinedResponse;
import org.spacehq.mc.auth.response.MinecraftProfilePropertiesResponse;
import org.spacehq.mc.auth.response.MinecraftTexturesPayload;
import org.spacehq.mc.auth.response.Response;
import org.spacehq.mc.auth.serialize.UUIDSerializer;
import org.spacehq.mc.auth.util.Base64;
import org.spacehq.mc.auth.util.IOUtils;
import org.spacehq.mc.auth.util.URLUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SessionService {

	private static final String BASE_URL = "https://sessionserver.mojang.com/session/minecraft/";
	private static final URL JOIN_URL = URLUtils.constantURL(BASE_URL + "join");
	private static final URL CHECK_URL = URLUtils.constantURL(BASE_URL + "hasJoined");

	private static final PublicKey SIGNATURE_KEY;
	private static final Gson GSON = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDSerializer()).create();

	static {
		try {
			X509EncodedKeySpec spec = new X509EncodedKeySpec(IOUtils.toByteArray(SessionService.class.getResourceAsStream("/yggdrasil_session_pubkey.der")));
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			SIGNATURE_KEY = keyFactory.generatePublic(spec);
		} catch(Exception e) {
			throw new ExceptionInInitializerError("Missing/invalid yggdrasil public key.");
		}
	}

	public void joinServer(GameProfile profile, String authenticationToken, String serverId) throws AuthenticationException {
		JoinServerRequest request = new JoinServerRequest(authenticationToken, profile.getId(), serverId);
		URLUtils.makeRequest(JOIN_URL, request, Response.class);
	}

	public GameProfile hasJoinedServer(GameProfile user, String serverId) throws AuthenticationUnavailableException {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("username", user.getName());
		arguments.put("serverId", serverId);
		URL url = URLUtils.concatenateURL(CHECK_URL, URLUtils.buildQuery(arguments));
		try {
			HasJoinedResponse response = URLUtils.makeRequest(url, null, HasJoinedResponse.class);
			if(response != null && response.getId() != null) {
				GameProfile result = new GameProfile(response.getId(), user.getName());
				if(response.getProperties() != null) {
					result.getProperties().putAll(response.getProperties());
				}

				return result;
			} else
				return null;
		} catch(AuthenticationUnavailableException e) {
			throw e;
		} catch(AuthenticationException e) {
			return null;
		}
	}

	public Map<ProfileTextureType, ProfileTexture> getTextures(GameProfile profile, boolean requireSecure) throws PropertyException {
		Property textures = profile.getProperties().get("textures");
		if(textures != null) {
			if(!textures.hasSignature())
				throw new ProfileTextureException("Signature is missing from textures payload.");

			if(!textures.isSignatureValid(SIGNATURE_KEY))
				throw new ProfileTextureException("Textures payload has been tampered with. (signature invalid)");

			MinecraftTexturesPayload result;
			try {
				String json = new String(Base64.decode(textures.getValue().getBytes("UTF-8")));
				result = GSON.fromJson(json, MinecraftTexturesPayload.class);
			} catch(Exception e) {
				throw new ProfileTextureException("Could not decode texture payload.", e);
			}

			if(result.getProfileId() == null || !result.getProfileId().equals(profile.getId()))
				throw new ProfileTextureException("Decrypted textures payload was for another user. (expected id " + profile.getId() + " but was for " + result.getProfileId() + ")");

				if(result.getProfileName() == null || !result.getProfileName().equals(profile.getName()))
					throw new ProfileTextureException("Decrypted textures payload was for another user. (expected name " + profile.getName() + " but was for " + result.getProfileName() + ")");
					if(requireSecure) {
						if(result.isPublic())
							throw new ProfileTextureException("Decrypted textures payload was public when secure data is required.");

						Calendar limit = Calendar.getInstance();
						limit.add(5, -1);
						Date validFrom = new Date(result.getTimestamp());
						if(validFrom.before(limit.getTime()))
							throw new ProfileTextureException("Decrypted textures payload is too old. (" + validFrom + ", needs to be at least " + limit + ")");
					}

					return result.getTextures() == null ? new HashMap<ProfileTextureType, ProfileTexture>() : result.getTextures();
		}

		return new HashMap<ProfileTextureType, ProfileTexture>();
	}

	public GameProfile fillProfileProperties(GameProfile profile) throws ProfileException {
		if(profile.getId() == null)
			return profile;

		try {
			URL url = URLUtils.constantURL("https://sessionserver.mojang.com/session/minecraft/profile/" + UUIDSerializer.fromUUID(profile.getId()));
			MinecraftProfilePropertiesResponse response = URLUtils.makeRequest(url, null, MinecraftProfilePropertiesResponse.class);
			if(response == null)
				throw new ProfileNotFoundException("Couldn't fetch profile properties for " + profile + " as the profile does not exist.");

			GameProfile result = new GameProfile(response.getId(), response.getName());
			result.getProperties().putAll(response.getProperties());
			profile.getProperties().putAll(response.getProperties());
			return result;
		} catch(AuthenticationException e) {
			throw new ProfileLookupException("Couldn't look up profile properties for " + profile, e);
		}
	}

	@Override
	public String toString() {
		return "SessionService{}";
	}

}
