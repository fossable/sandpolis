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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sandpolis.core.attribute.key.AK_VIEWER;
import com.sandpolis.core.instance.Signaler;
import com.sandpolis.core.instance.storage.MemoryListStoreProvider;
import com.sandpolis.core.instance.storage.StoreProvider;
import com.sandpolis.core.instance.storage.StoreProviderFactory;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.instance.store.StoreBase;
import com.sandpolis.core.profile.ProfileStore.ProfileStoreConfig;
import com.sandpolis.core.proto.net.MCDelta.EV_ProfileDelta;
import com.sandpolis.core.proto.util.Platform.Instance;

/**
 * @author cilki
 * @since 4.0.0
 */
public final class ProfileStore extends StoreBase<ProfileStoreConfig> {

	private static StoreProvider<Profile> provider;

	/**
	 * The {@link StoreProvider}'s backing container if configured with
	 * {@link #load(List)}.
	 */
	private static List<Profile> providerContainer;

	public static void init(StoreProvider<Profile> provider) {
		ProfileStore.provider = Objects.requireNonNull(provider);
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

	public enum Events {
		PROFILE_ONLINE, PROFILE_OFFLINE;
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
	 * Get an existing {@link Profile} from the store or create a new one.
	 * 
	 * @param cvid The profile CVID
	 * @param uuid The profile UUID
	 * @return A new or existing profile
	 */
	public static Profile getProfileOrCreate(int cvid, String uuid) {
		Profile profile = provider.get("uuid", uuid).orElse(null);
		if (profile == null) {
			profile = new Profile(cvid, uuid);
			provider.add(profile);
			Signaler.fire(Events.PROFILE_ONLINE, profile);
		}
		return profile;
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
		return provider.stream().filter(profile -> username.equals(profile.get(AK_VIEWER.USERNAME))).findFirst();
	}

	public static void merge(List<EV_ProfileDelta> updates) throws Exception {
		for (EV_ProfileDelta update : updates) {
			Profile profile = getProfile(update.getCvid()).orElse(null);
			if (profile == null) {
				profile = new Profile(update.getCvid(), "TODO");// TODO

				profile.merge(update.getUpdate());
				provider.add(profile);
			} else {
				profile.merge(update.getUpdate());
			}
		}
	}

	public static List<EV_ProfileDelta> getUpdates(long timestamp, int cvid) {
		return provider.stream().filter(profile -> profile.getInstance() == Instance.CLIENT)// TODO filter permissions
				.map(profile -> EV_ProfileDelta.newBuilder().setUpdate(profile.getUpdates(timestamp)).build())
				// TODO set cvid or uuid in update
				.collect(Collectors.toList());

	}

	public static Stream<Profile> getProfiles() {
		return provider.stream();
	}

	public static final class ProfileStoreConfig {

	}

	public static final ProfileStore ProfileStore = new ProfileStore();

	@Override
	public void init(Consumer<ProfileStoreConfig> o) {
		// TODO Auto-generated method stub
		
	}

}
