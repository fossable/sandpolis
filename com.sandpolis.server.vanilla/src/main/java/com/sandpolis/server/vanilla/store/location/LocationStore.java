//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.server.vanilla.store.location;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sandpolis.core.instance.store.StoreBase;
import com.sandpolis.core.proto.util.LocationOuterClass;
import com.sandpolis.core.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sandpolis.server.vanilla.store.location.LocationStore.LocationStoreConfig;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class LocationStore extends StoreBase<LocationStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(LocationStore.class);

	private Cache<String, LocationOuterClass.Location> cache;

	private AbstractGeolocationService service;

	public LocationStore() {
		super(log);
	}

	public Future<LocationOuterClass.Location> queryAsync(String ip) {
		// Private IPs should not be resolved
		if (ValidationUtil.privateIP(ip)) {
			return CompletableFuture.completedFuture(null);
		}

		// Check cache
		synchronized (cache) {
			var location = cache.getIfPresent(ip);
			if (location != null)
				return CompletableFuture.completedFuture(location);
		}

		return service.query(ip, service.attrMap.keySet()).thenApply(location -> {
			if (location != null) {
				synchronized (cache) {
					cache.put(ip, location);
				}
			}
			return location;
		});
	}

	public LocationOuterClass.Location query(String ip, long timeout) {
		try {
			return queryAsync(ip).get(timeout, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			log.error("Failed to query location service", e);
			return null;
		}
	}

	@Override
	public LocationStore init(Consumer<LocationStoreConfig> configurator) {
		var config = new LocationStore.LocationStoreConfig();
		configurator.accept(config);

		// Initialize cache
		cache = CacheBuilder.newBuilder().expireAfterWrite(config.cacheExpiration).build();

		// Initialize location service
		switch (config.service) {
		case "ip-api.com":
			if (config.key == null) {
				service = new IpApi();
			} else {
				service = new IpApi(config.key);
			}
			break;
		case "tools.keycdn.com":
			service = new KeyCdn();
			break;
		}

		return (LocationStore) super.init(null);
	}

	public final class LocationStoreConfig extends StoreBase.StoreConfig {
		public String service;
		public String key;
		public Duration cacheExpiration;
	}

	public static final LocationStore LocationStore = new LocationStore();
}
