package com.gameminers.visage.master.cache;

import com.typesafe.config.Config;

public class JCSCacheManager extends CacheManager {
	private Config config;
	public JCSCacheManager(Config config) {
		this.config = config;
	}
	
}

