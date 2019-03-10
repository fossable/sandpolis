/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.core.profile;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sandpolis.core.instance.Store;
import com.sandpolis.core.instance.Store.ManualInitializer;
import com.sandpolis.core.instance.storage.MemoryListStoreProvider;
import com.sandpolis.core.instance.storage.StoreProvider;
import com.sandpolis.core.instance.storage.StoreProviderFactory;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.proto.net.MCDelta.EV_ProfileDelta;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.util.IDUtil;

/**
 * @author cilki
 * @since 4.0.0
 */
@ManualInitializer
public final class ProfileStore extends Store {

	/**
	 * How long to keep profiles loaded in minutes.
	 */
	public static final int PROFILE_EXPIRATION = 15;

	private static StoreProvider<Profile> provider;

	/**
	 * The {@link StoreProvider}'s backing container if configured with
	 * {@link #load(Database)}.
	 */
	private static List<Profile> providerContainer;

	/**
	 * Recently used profiles that are mapped to a CVID.
	 */
	private static Cache<Integer, Profile> profileCache;

	public static void init(StoreProvider<Profile> provider) {
		ProfileStore.provider = Objects.requireNonNull(provider);

		// Load profile cache
		profileCache = CacheBuilder.newBuilder().expireAfterAccess(PROFILE_EXPIRATION, TimeUnit.MINUTES).build();
	}

	public static void load(Database main) {
		Objects.requireNonNull(main);

		init(StoreProviderFactory.database(Profile.class, main));
	}

	/**
	 * Initialize the store and expose its backing container.
	 * 
	 * @param container The container to use when building the {@link StoreProvider}
	 */
	public static void load(List<Profile> container) {
		ProfileStore.providerContainer = Objects.requireNonNull(container);

		init(new MemoryListStoreProvider<Profile>(Profile.class, container));
	}

	/**
	 * Get the {@link StoreProvider}'s backing container if the store was configured
	 * with {@link #load(List)}.
	 * 
	 * @return The store's backing container or {@code null} if access to the
	 *         container is not allowed
	 */
	public static List<Profile> getContainer() {
		return providerContainer;
	}

	/**
	 * Retrieve a {@link Profile} by CVID.
	 * 
	 * @param cvid The profile's CVID
	 * @return The requested {@link Profile}
	 */
	public static Optional<Profile> getProfile(int cvid) {
		return provider.get("cvid", cvid);
	}

	/**
	 * Retrieve a viewer's {@link Profile} by username.
	 * 
	 * @param username The username of the requested profile
	 * @return The requested {@link Profile}
	 */
	public static Optional<Profile> getViewer(String username) {
		// TODO
		return null;
	}

	public static void merge(List<EV_ProfileDelta> updates) throws Exception {
		for (EV_ProfileDelta update : updates) {
			Profile profile = getProfile(update.getCvid()).orElse(null);
			if (profile == null) {
				profile = new Profile(IDUtil.CVID.extractInstance(update.getCvid()));

				profile.merge(update.getUpdate());
				provider.add(profile);
			} else {
				profile.merge(update.getUpdate());
			}
		}
	}

	public static List<EV_ProfileDelta> getUpdates(long timestamp, int cvid) {
		try (Stream<Profile> stream = provider.stream()) {
			return stream.filter(profile -> profile.getInstance() == Instance.CLIENT)// TODO filter permissions
					.map(profile -> EV_ProfileDelta.newBuilder().setUpdate(profile.getUpdates(timestamp)).build())
					// TODO set cvid or uuid in update
					.collect(Collectors.toList());
		}

	}

}
