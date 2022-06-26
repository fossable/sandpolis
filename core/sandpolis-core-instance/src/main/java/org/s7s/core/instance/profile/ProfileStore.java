//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.profile;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.instance.Entrypoint;
import org.s7s.core.instance.pref.PrefStore.PrefStoreConfig;
import org.s7s.core.instance.profile.ProfileStore.ProfileStoreConfig;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ClientOid;
import org.s7s.core.instance.state.InstanceOids.ProfileOid;
import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.store.ConfigurableStore;
import org.s7s.core.instance.store.STCollectionStore;

/**
 * {@link ProfileStore} manages profiles which represent any type of instance.
 *
 * @since 4.0.0
 */
public final class ProfileStore extends STCollectionStore<Profile> implements ConfigurableStore<ProfileStoreConfig> {

	public static final record ProfileOnlineEvent(Profile profile) {
	}

	public static final record ProfileOfflineEvent(Profile profile) {
	}

	public static final class ProfileStoreConfig {

		public STDocument collection;

		private ProfileStoreConfig(Consumer<ProfileStoreConfig> configurator) {
			configurator.accept(this);
		}
	}

	private static final Logger log = LoggerFactory.getLogger(ProfileStore.class);

	public static final ProfileStore ProfileStore = new ProfileStore();

	private ProfileStore() {
		super(log, Profile::new);
	}

	public STDocument instance() {
		return collection.document(Entrypoint.data().uuid());
	}

	/**
	 * Get a profile by SID.
	 *
	 * @param sid The profile SID
	 * @return The requested profile
	 */
	public Optional<Profile> getBySid(int sid) {
		return values().stream().filter(profile -> profile.get(ProfileOid.SID).isPresent())
				.filter(profile -> profile.get(ProfileOid.SID).asInt() == sid).findFirst();
	}

	/**
	 * Get a profile by UUID.
	 *
	 * @param uuid The profile UUID
	 * @return The requested profile
	 */
	public Optional<Profile> getByUuid(String uuid) {
		requireNonNull(uuid);
		return values().stream().filter(profile -> uuid.equals(profile.get(ProfileOid.UUID).asString())).findFirst();
	}

	/**
	 * Get a client profile by username.
	 *
	 * @param username The profile username
	 * @return The requested profile
	 */
	public Optional<Profile> getClient(String username) {
		requireNonNull(username);
		return values().stream().filter(profile -> profile.get(ClientOid.USERNAME).isPresent())
				.filter(profile -> username.equals(profile.get(ClientOid.USERNAME).asString())).findFirst();
	}

	@Override
	public void init(Consumer<ProfileStoreConfig> configurator) {

		var config = new ProfileStoreConfig(configurator);

		// Apply configuration
		setDocument(config.collection);

		// Create the local instance if it doesn't exist
		if (getByUuid(Entrypoint.data().uuid()).isEmpty()) {
			create(profile -> {
				profile.set(ProfileOid.UUID, Entrypoint.data().uuid());
			});
		}
	}
}
