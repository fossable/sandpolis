//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.instance.profile;

import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.foundation.ConfigStruct;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.profile.ProfileStore.ProfileStoreConfig;
import com.sandpolis.core.instance.state.VirtProfile;
import com.sandpolis.core.instance.state.vst.VirtCollection;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.STCollectionStore;

/**
 * {@link ProfileStore} manages profiles which represent any type of instance.
 *
 * @since 4.0.0
 */
public final class ProfileStore extends STCollectionStore<Profile> implements ConfigurableStore<ProfileStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(ProfileStore.class);

	public ProfileStore() {
		super(log);
	}

	/**
	 * Retrieve a client's {@link Profile} by username.
	 *
	 * @param username The username of the requested profile
	 * @return The requested {@link Profile}
	 */
	public Optional<Profile> getClient(String username) {
		return values().stream().filter(profile -> username.equals(profile.client().getUsername())).findFirst();
	}

	/**
	 * Get a profile by cvid.
	 *
	 * @param cvid The profile CVID
	 * @return The profile
	 */
	public Optional<Profile> getByCvid(int cvid) {
		return values().stream().filter(profile -> profile.cvid().isPresent())
				.filter(profile -> profile.getCvid() == cvid).findFirst();
	}

	public Optional<Profile> getByUuid(String uuid) {
		return values().stream().filter(profile -> uuid.equals(profile.getUuid())).findFirst();
	}

	@Override
	public void init(Consumer<ProfileStoreConfig> configurator) {
		var config = new ProfileStoreConfig();
		configurator.accept(config);

		collection = config.collection;

		// Create the local instance if it doesn't exist
		create(profile -> {
			profile.uuid().set(Core.UUID);
		});
	}

	public Profile create(Consumer<VirtProfile> configurator) {
		return add(configurator, Profile::new);
	}

	@ConfigStruct
	public static final class ProfileStoreConfig {

		public VirtCollection<Profile> collection;
	}

	public static final ProfileStore ProfileStore = new ProfileStore();
}
