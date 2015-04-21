package com.gameminers.visage.master.cache;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.spacehq.mc.auth.GameProfile;

import com.typesafe.config.Config;

public class JCSCacheManager implements CacheManager {
	private Config config;
	public JCSCacheManager(Config config) {
		this.config = config;
	}
	
	@Override
	public boolean hasUsername(String name) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public UUID getUUID(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GameProfile getProfile(UUID uuid, Callable<GameProfile> loader)
			throws ExecutionException {
		// TODO Auto-generated method stub
		return null;
	}
	
}

