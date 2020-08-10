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

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.profile.ProfileStore.ProfileStoreConfig;
import com.sandpolis.core.instance.state.Document;
import com.sandpolis.core.instance.store.CollectionStore;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.core.instance.store.provider.MemoryListStoreProvider;
import com.sandpolis.core.instance.store.provider.MemoryMapStoreProvider;
import com.sandpolis.core.instance.store.provider.StoreProviderFactory;

/**
 * @author cilki
 * @since 4.0.0
 */
public final class ProfileStore extends CollectionStore<Profile, ProfileStoreConfig> {

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

	@Override
	public Profile create(Consumer<Profile> configurator) {
		var profile = new Profile(new Document(provider.getCollection()));
		configurator.accept(profile);
		provider.add(profile);
		return profile;
	}

	public final class ProfileStoreConfig extends StoreConfig {

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(Profile.class, Profile::tag);
		}

		public void ephemeral(List<Profile> list) {
			container = list;
			provider = new MemoryListStoreProvider<>(Profile.class, Profile::tag, list);
		}

		@Override
		public void persistent(StoreProviderFactory factory) {
			provider = factory.supply(Profile.class, Profile::new);
		}
	}

	public static final ProfileStore ProfileStore = new ProfileStore();
}
