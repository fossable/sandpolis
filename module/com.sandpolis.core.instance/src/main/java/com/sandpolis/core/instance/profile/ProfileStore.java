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

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.foundation.ConfigStruct;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.profile.ProfileStore.ProfileStoreConfig;
import com.sandpolis.core.instance.state.ClientOid;
import com.sandpolis.core.instance.state.ProfileOid;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.STCollectionStore;

/**
 * {@link ProfileStore} manages profiles which represent any type of instance.
 *
 * @since 4.0.0
 */
public final class ProfileStore extends STCollectionStore<Profile> implements ConfigurableStore<ProfileStoreConfig> {

	@ConfigStruct
	public static final class ProfileStoreConfig {

		public STDocument collection;
	}

	private static final Logger log = LoggerFactory.getLogger(ProfileStore.class);

	public static final ProfileStore ProfileStore = new ProfileStore();

	private ProfileStore() {
		super(log, Profile::new);
	}

	/**
	 * Get a profile by CVID.
	 *
	 * @param cvid The profile CVID
	 * @return The requested profile
	 */
	public Optional<Profile> getByCvid(int cvid) {
		return values().stream().filter(profile -> profile.attribute(ProfileOid.CVID).isPresent())
				.filter(profile -> profile.get(ProfileOid.CVID) == cvid).findFirst();
	}

	/**
	 * Get a profile by UUID.
	 * 
	 * @param uuid The profile UUID
	 * @return The requested profile
	 */
	public Optional<Profile> getByUuid(String uuid) {
		requireNonNull(uuid);
		return values().stream().filter(profile -> uuid.equals(profile.get(ProfileOid.UUID))).findFirst();
	}

	/**
	 * Get a client profile by username.
	 *
	 * @param username The profile username
	 * @return The requested profile
	 */
	public Optional<Profile> getClient(String username) {
		requireNonNull(username);
		return values().stream().filter(profile -> username.equals(profile.get(ClientOid.USERNAME))).findFirst();
	}

	@Override
	public void init(Consumer<ProfileStoreConfig> configurator) {

		// Prepare configuration
		var config = new ProfileStoreConfig();
		configurator.accept(config);

		// Apply configuration
		setDocument(config.collection);

		// Create the local instance if it doesn't exist
		if (getByUuid(Core.UUID).isEmpty()) {
			create(profile -> {
				profile.set(ProfileOid.UUID, Core.UUID);
			});
		}
	}
}
