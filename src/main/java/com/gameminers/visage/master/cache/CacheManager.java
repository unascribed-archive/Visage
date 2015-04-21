package com.gameminers.visage.master.cache;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.spacehq.mc.auth.GameProfile;

public interface CacheManager {

	boolean hasUsername(String name);
	UUID getUUID(String name);
	
	GameProfile getProfile(UUID uuid, Callable<GameProfile> loader) throws ExecutionException;
	
}
