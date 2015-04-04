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

import org.spacehq.mc.auth.GameProfile;
import org.spacehq.mc.auth.response.ProfileSearchResultsResponse;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class ProfileSearchResultsSerializer implements JsonDeserializer<ProfileSearchResultsResponse> {
	@Override
	public ProfileSearchResultsResponse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		ProfileSearchResultsResponse result = new ProfileSearchResultsResponse();
		if(json instanceof JsonObject) {
			JsonObject object = (JsonObject) json;
			if(object.has("error")) {
				result.setError(object.getAsJsonPrimitive("error").getAsString());
			}

			if(object.has("errorMessage")) {
				result.setError(object.getAsJsonPrimitive("errorMessage").getAsString());
			}

			if(object.has("cause")) {
				result.setError(object.getAsJsonPrimitive("cause").getAsString());
			}
		} else {
			result.setProfiles((GameProfile[]) context.deserialize(json, GameProfile[].class));
		}

		return result;
	}
}
