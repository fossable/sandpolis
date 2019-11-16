/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
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
		// Check for private IP
		if (ValidationUtil.privateIP(ip)) {
			return CompletableFuture.completedFuture(null);
		}

		// Check cache
		var location = cache.getIfPresent(ip);
		if (location != null)
			return CompletableFuture.completedFuture(location);

		return service.query(ip, service.attrMap.keySet());
	}

	public LocationOuterClass.Location query(String ip, long timeout) {
		try {
			return queryAsync(ip).get(timeout, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			e.printStackTrace();
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
