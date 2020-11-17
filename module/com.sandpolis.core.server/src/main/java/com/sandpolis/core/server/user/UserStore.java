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
package com.sandpolis.core.server.user;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.sandpolis.core.foundation.ConfigStruct;
import com.sandpolis.core.foundation.util.CryptoUtil;
import com.sandpolis.core.instance.User.UserConfig;
import com.sandpolis.core.instance.state.VirtUser;
import com.sandpolis.core.instance.state.vst.VirtCollection;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.STCollectionStore;
import com.sandpolis.core.server.user.UserStore.UserStoreConfig;

public final class UserStore extends STCollectionStore<User> implements ConfigurableStore<UserStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(UserStore.class);

	public UserStore() {
		super(log);
	}

	public Optional<User> getByUsername(String username) {
		return values().stream().filter(user -> user.getUsername().equals(username)).findAny();
	}

	public Optional<User> getByCvid(int cvid) {
		return null;
	}

	@Override
	public void init(Consumer<UserStoreConfig> configurator) {
		var config = new UserStoreConfig();
		configurator.accept(config);

		collection = config.collection;
	}

	public User create(Consumer<VirtUser> configurator) {
		return add(configurator, User::new);
	}

	/**
	 * Create a new user from the given configuration.
	 *
	 * @param config The user configuration
	 */
	public User create(UserConfig config) {
		Objects.requireNonNull(config);

		return create(user -> {
			user.username().set(config.getUsername());
			user.email().set(config.getEmail());
			user.expiration().set(config.getExpiration());
			user.hash().set(CryptoUtil.PBKDF2.hash(
					// Compute a preliminary hash before PBKDF2 is applied
					Hashing.sha512().hashString(config.getPassword(), Charsets.UTF_8).toString()));
		});
	}

	@ConfigStruct
	public static final class UserStoreConfig {

		public VirtCollection<VirtUser> collection;
	}

	/**
	 * The global context {@link UserStore}.
	 */
	public static final UserStore UserStore = new UserStore();
}
