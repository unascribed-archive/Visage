package com.gameminers.visage.master.cache;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.spacehq.mc.auth.GameProfile;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class GuavaBasicCacheManager implements CacheManager {
	private Cache<UUID, GameProfile> cache;
	public GuavaBasicCacheManager() {
		cache = CacheBuilder.newBuilder()
				.maximumSize(1000)
				.expireAfterWrite(1, TimeUnit.MINUTES)
				.build();
		new Timer("Guava cache cleaner", true).schedule(new TimerTask() {
			
			@Override
			public void run() {
				cache.cleanUp();
			}
		}, 60000);
	}
	@Override
	public GameProfile getProfile(UUID uuid, Callable<GameProfile> loader) throws ExecutionException {
		return cache.get(uuid, loader);
	}
	
	// Stubs
	////
	// The GuavaBasicCacheManager is the representation of no caching, so we
	// only cache that which we absolutely must.
	////
	@Override public boolean hasUsername(String name) { return false; }
	@Override public UUID getUUID(String name) { return null; }

}
