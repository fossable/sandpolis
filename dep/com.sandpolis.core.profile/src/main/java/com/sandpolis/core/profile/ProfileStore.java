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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Store;
import com.sandpolis.core.instance.Store.ManualInitializer;
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
	 * Retrieve a {@link Profile} by CVID.
	 * 
	 * @param cvid The profile's CVID
	 * @return The requested {@link Profile} or {@code null} if not found
	 */
	public static Profile getProfile(int cvid) {
		if (!profileCache.asMap().containsKey(cvid)) {
			// Cache miss
			Profile profile = provider.get("cvid", cvid);
			if (profile == null)
				return null;
			profileCache.put(cvid, profile);
		}
		return profileCache.getIfPresent(cvid);
	}

	/**
	 * Retrieve a viewer's {@link Profile} by username.
	 * 
	 * @param username The username of the requested profile
	 * @return The requested {@link Profile} or {@code null} if not found
	 */
	public static Profile getViewer(String username) {
		// TODO
		return null;
	}

	public static void merge(List<EV_ProfileDelta> updates) {
		for (EV_ProfileDelta update : updates) {
			Profile profile = getProfile(update.getCvid());
			if (profile == null) {
				profile = new Profile(IDUtil.CVID.extractInstance(update.getCvid()));

				try {
					profile.merge(update.getUpdate());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				provider.add(profile);
			} else {
				try {
					profile.merge(update.getUpdate());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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

	public static Profile getLocalProfile() {
		return getProfile(Core.cvid());
	}

}
