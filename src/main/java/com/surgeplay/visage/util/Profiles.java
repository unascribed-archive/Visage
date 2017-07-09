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

package com.surgeplay.visage.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.data.GameProfile.Texture;
import com.github.steveice10.mc.auth.data.GameProfile.TextureModel;
import com.github.steveice10.mc.auth.data.GameProfile.TextureType;
import com.google.common.collect.Maps;

public class Profiles {
	public static boolean isSlim(GameProfile profile) throws IOException {
		System.out.println(profile);
		if (profile.getTextures().containsKey(TextureType.SKIN)) {
			Texture t = profile.getTexture(TextureType.SKIN);
			return t.getModel() == TextureModel.SLIM;
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
			TextureType type = TextureType.values()[data.readUnsignedByte()];
			TextureModel model = TextureModel.values()[data.readUnsignedByte()];
			String url = data.readUTF();
			Map<String, String> m = Maps.newHashMap();
			m.put("model", model.name().toLowerCase(Locale.ROOT));
			profile.getTextures().put(type, new Texture(url, m));
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
		data.writeShort(profile.getTextures().size());
		for (Map.Entry<TextureType, Texture> en : profile.getTextures().entrySet()) {
			data.writeByte(en.getKey().ordinal());
			data.writeByte(en.getValue().getModel().ordinal());
			data.writeUTF(en.getValue().getURL());
		}
	}
}
