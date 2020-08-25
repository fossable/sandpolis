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
package com.sandpolis.core.instance.profile;

import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.StateTree.VirtProfile;
import com.sandpolis.core.instance.profile.ProfileStore.ProfileStoreConfig;
import com.sandpolis.core.instance.state.STStore;
import com.sandpolis.core.instance.store.CollectionStore;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.core.instance.store.provider.MemoryMapStoreProvider;
import com.sandpolis.core.instance.store.provider.StoreProviderFactory;

/**
 * {@link ProfileStore} manages profiles that can represent any type of
 * instance.
 *
 * @since 4.0.0
 */
public final class ProfileStore extends CollectionStore<Profile> implements ConfigurableStore<ProfileStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(ProfileStore.class);

	public ProfileStore() {
		super(log);
	}

	/**
	 * Retrieve a viewer's {@link Profile} by username.
	 *
	 * @param username The username of the requested profile
	 * @return The requested {@link Profile}
	 */
	public Optional<Profile> getViewer(String username) {
		return provider.stream().filter(profile -> username.equals(profile.viewer().getUsername())).findFirst();
	}

	public Profile local() {
		return null;
	}

	/**
	 * Get a profile by cvid.
	 *
	 * @param cvid The profile CVID
	 * @return The profile
	 */
	public Optional<Profile> get(int cvid) {
		return stream().filter(profile -> profile.getCvid() == cvid).findFirst();
	}

	public Optional<Profile> getByUuid(String uuid) {
		return stream().filter(profile -> profile.getUuid().equals(uuid)).findFirst();
	}

	@Override
	public void init(Consumer<ProfileStoreConfig> configurator) {
		var config = new ProfileStoreConfig();
		configurator.accept(config);

		provider.initialize();
	}

	public Profile create(Consumer<Profile> configurator) {
		return add(new Profile(STStore.newRootDocument()), configurator);
	}

	public final class ProfileStoreConfig extends StoreConfig {

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(Profile.class, Profile::tag, VirtProfile.DOCUMENT);
		}

		@Override
		public void persistent(StoreProviderFactory factory) {
			provider = factory.supply(Profile.class, Profile::new, VirtProfile.DOCUMENT);
		}
	}

	public static final ProfileStore ProfileStore = new ProfileStore();
}
