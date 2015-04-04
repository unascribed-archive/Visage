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
package org.spacehq.mc.auth.serialize;

import java.lang.reflect.Type;
import java.util.UUID;

import org.spacehq.mc.auth.GameProfile;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GameProfileSerializer implements JsonSerializer<GameProfile>, JsonDeserializer<GameProfile> {

	@Override
	public GameProfile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject object = (JsonObject) json;
		UUID id = object.has("id") ? (UUID) context.deserialize(object.get("id"), UUID.class) : null;
		String name = object.has("name") ? object.getAsJsonPrimitive("name").getAsString() : null;
		return new GameProfile(id, name);
	}

	@Override
	public JsonElement serialize(GameProfile src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject result = new JsonObject();
		if(src.getId() != null) {
			result.add("id", context.serialize(src.getId()));
		}

		if(src.getName() != null) {
			result.addProperty("name", src.getName());
		}

		return result;
	}

}
