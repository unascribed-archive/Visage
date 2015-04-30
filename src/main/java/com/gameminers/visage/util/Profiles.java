package com.gameminers.visage.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.UUID;

import org.spacehq.mc.auth.GameProfile;
import org.spacehq.mc.auth.properties.Property;
import org.spacehq.mc.auth.serialize.UUIDSerializer;
import org.spacehq.mc.auth.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class Profiles {
	private static Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDSerializer()).create();

	public static boolean isSlim(GameProfile profile) throws IOException {
		if (profile.getProperties().containsKey("textures")) {
			String texJson = new String(Base64.decode(profile.getProperties().get("textures").getValue().getBytes(StandardCharsets.UTF_8)));
			JsonObject obj = gson.fromJson(texJson, JsonObject.class);
			JsonObject tex = obj.getAsJsonObject("textures");
			if (tex.has("SKIN")) {
				JsonObject skin = tex.getAsJsonObject("SKIN");
				if (skin.has("metadata")) {
					if ("slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString()))
						return true;
				}
				return false;
			}
		}
		return UUIDs.isAlex(profile.getId());
	}

	public static GameProfile readGameProfile(DataInputStream data) throws IOException {
		boolean present = data.readBoolean();
		if (!present)
			return new GameProfile(new UUID(0, 0), "<unknown>");
		UUID uuid = new UUID(data.readLong(), data.readLong());
		String name = data.readUTF();
		GameProfile profile = new GameProfile(uuid, name);
		int len = data.readUnsignedShort();
		for (int i = 0; i < len; i++) {
			boolean signed = data.readBoolean();
			Property prop;
			if (signed) {
				prop = new Property(data.readUTF(), data.readUTF(), data.readUTF());
			} else {
				prop = new Property(data.readUTF(), data.readUTF());
			}
			profile.getProperties().put(data.readUTF(), prop);
		}
		return profile;
	}

	public static void writeGameProfile(DataOutputStream data, GameProfile profile) throws IOException {
		if (profile == null) {
			data.writeBoolean(false);
			return;
		}
		data.writeBoolean(true);
		data.writeLong(profile.getId().getMostSignificantBits());
		data.writeLong(profile.getId().getLeastSignificantBits());
		data.writeUTF(profile.getName());
		data.writeShort(profile.getProperties().size());
		for (Entry<String, Property> en : profile.getProperties().entrySet()) {
			data.writeBoolean(en.getValue().hasSignature());
			if (en.getValue().hasSignature()) {
				data.writeUTF(en.getValue().getName());
				data.writeUTF(en.getValue().getValue());
				data.writeUTF(en.getValue().getSignature());
			} else {
				data.writeUTF(en.getValue().getName());
				data.writeUTF(en.getValue().getValue());
			}
			data.writeUTF(en.getKey());
		}
	}
}
