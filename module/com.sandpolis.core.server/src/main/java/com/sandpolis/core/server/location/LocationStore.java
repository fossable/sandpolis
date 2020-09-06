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
package com.sandpolis.core.server.location;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sandpolis.core.foundation.ConfigStruct;
import com.sandpolis.core.foundation.util.ValidationUtil;
import com.sandpolis.core.instance.state.VirtIpLocation;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.StoreBase;
import com.sandpolis.core.server.location.LocationStore.LocationStoreConfig;
import com.sandpolis.core.server.location.services.IpApi;
import com.sandpolis.core.server.location.services.KeyCdn;

public class LocationStore extends StoreBase implements ConfigurableStore<LocationStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(LocationStore.class);

	private Cache<String, VirtIpLocation> cache;

	private AbstractGeolocationService service;

	public LocationStore() {
		super(log);
	}

	public Future<VirtIpLocation> queryAsync(String ip, Oid... fields) {
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

		return service.query(ip, fields).thenApply(location -> {
			if (location != null) {
				synchronized (cache) {
					cache.put(ip, location);
				}
			}
			return location;
		});
	}

	public VirtIpLocation query(String ip, long timeout) {
		try {
			return queryAsync(ip).get(timeout, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			log.error("Failed to query location service: timed out");
			return null;
		} catch (Exception e) {
			log.error("Failed to query location service", e);
			return null;
		}
	}

	@Override
	public void init(Consumer<LocationStoreConfig> configurator) {
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
	}

	@ConfigStruct
	public static final class LocationStoreConfig {

		/**
		 * The location service.
		 */
		public String service;

		/**
		 * The service API key.
		 */
		public String key;

		/**
		 * The amount of time location queries are cached.
		 */
		public Duration cacheExpiration;
	}

	public static final LocationStore LocationStore = new LocationStore();
}
