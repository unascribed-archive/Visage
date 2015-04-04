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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.spacehq.mc.auth.exception.AuthenticationException;
import org.spacehq.mc.auth.exception.ProfileNotFoundException;
import org.spacehq.mc.auth.response.ProfileSearchResultsResponse;
import org.spacehq.mc.auth.util.URLUtils;

public class GameProfileRepository {

	private static final String BASE_URL = "https://api.mojang.com/";
	private static final String SEARCH_URL = BASE_URL + "profiles/minecraft";
	private static final int MAX_FAIL_COUNT = 3;
	private static final int DELAY_BETWEEN_PAGES = 100;
	private static final int DELAY_BETWEEN_FAILURES = 750;
	private static final int PROFILES_PER_REQUEST = 100;

	public void findProfilesByNames(String[] names, ProfileLookupCallback callback) {
		Set<String> criteria = new HashSet<String>();
		for(String name : names) {
			if(name != null && !name.isEmpty()) {
				criteria.add(name.toLowerCase());
			}
		}

		for(Set<String> request : partition(criteria, PROFILES_PER_REQUEST)) {
			Exception error = null;
			int failCount = 0;
			boolean tryAgain = true;
			while(failCount < MAX_FAIL_COUNT && tryAgain) {
				tryAgain = false;
				try {
					ProfileSearchResultsResponse response = URLUtils.makeRequest(URLUtils.constantURL(SEARCH_URL), request, ProfileSearchResultsResponse.class);
					failCount = 0;
					error = null;
					Set<String> missing = new HashSet<String>(request);
					for(GameProfile profile : response.getProfiles()) {
						missing.remove(profile.getName().toLowerCase());
						callback.onProfileLookupSucceeded(profile);
					}

					for(String name : missing) {
						callback.onProfileLookupFailed(new GameProfile((UUID) null, name), new ProfileNotFoundException("Server could not find the requested profile."));
					}

					try {
						Thread.sleep(DELAY_BETWEEN_PAGES);
					} catch(InterruptedException ignored) {
					}
				} catch(AuthenticationException e) {
					error = e;
					failCount++;
					if(failCount >= MAX_FAIL_COUNT) {
						for(String name : request) {
							callback.onProfileLookupFailed(new GameProfile((UUID) null, name), error);
						}
					} else {
						try {
							Thread.sleep(DELAY_BETWEEN_FAILURES);
						} catch(InterruptedException ignored) {
						}

						tryAgain = true;
					}
				}
			}
		}
	}

	private static Set<Set<String>> partition(Set<String> set, int size) {
		List<String> list = new ArrayList<String>(set);
		Set<Set<String>> ret = new HashSet<Set<String>>();
		for(int i = 0; i < list.size(); i += size) {
			Set<String> s = new HashSet<String>();
			s.addAll(list.subList(i, Math.min(i + size, list.size())));
			ret.add(s);
		}

		return ret;
	}
}
