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
package com.sandpolis.core.profile.store;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.storage.MemoryListStoreProvider;
import com.sandpolis.core.instance.storage.MemoryMapStoreProvider;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.instance.store.StoreBase.StoreConfig;
import com.sandpolis.core.profile.AK_VIEWER;
import com.sandpolis.core.profile.store.ProfileStore.ProfileStoreConfig;

/**
 * @author cilki
 * @since 4.0.0
 */
public final class ProfileStore extends MapStore<String, Profile, ProfileStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(ProfileStore.class);

	private Object container;

	public ProfileStore() {
		super(log);
	}

	public <E> E getContainer() {
		return (E) container;
	}

	/**
	 * Retrieve a viewer's {@link Profile} by username.
	 *
	 * @param username The username of the requested profile
	 * @return The requested {@link Profile}
	 */
	public Optional<Profile> getViewer(String username) {
		return provider.stream().filter(profile -> username.equals(profile.get(AK_VIEWER.USERNAME))).findFirst();
	}

//	public void merge(List<EV_ProfileDelta> updates) throws Exception {
//		for (EV_ProfileDelta update : updates) {
//			Profile profile = get(update.getCvid()).orElse(null);
//			if (profile == null) {
//				profile = new Profile(update.getCvid(), "TODO");// TODO
//
//				profile.merge(update.getUpdate());
//				provider.add(profile);
//			} else {
//				profile.merge(update.getUpdate());
//			}
//		}
//	}
//
//	public List<EV_ProfileDelta> getUpdates(long timestamp, int cvid) {
//		return provider.stream().filter(profile -> profile.getInstance() == Instance.CLIENT)// TODO filter permissions
//				.map(profile -> EV_ProfileDelta.newBuilder().setUpdate(profile.getUpdates(timestamp)).build())
//				// TODO set cvid or uuid in update
//				.collect(Collectors.toList());
//
//	}

	/**
	 * Get a profile by cvid.
	 *
	 * @param cvid The profile CVID
	 * @return The profile
	 */
	public Optional<Profile> get(int cvid) {
		return stream().filter(profile -> profile.getCvid() == cvid).findFirst();
	}

	@Override
	public ProfileStore init(Consumer<ProfileStoreConfig> configurator) {
		var config = new ProfileStoreConfig();
		configurator.accept(config);

		return (ProfileStore) super.init(null);
	}

	public final class ProfileStoreConfig extends StoreConfig {

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(Profile.class, Profile::getUuid);
		}

		public void ephemeral(List<Profile> list) {
			container = list;
			provider = new MemoryListStoreProvider<>(Profile.class, Profile::getUuid, list);
		}

		@Override
		public void persistent(Database database) {
			provider = database.getConnection().provider(Profile.class, "uuid");
		}
	}

	public static final ProfileStore ProfileStore = new ProfileStore();
}
