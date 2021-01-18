//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
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
import com.sandpolis.core.instance.state.UserOid;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.STCollectionStore;
import com.sandpolis.core.server.user.UserStore.UserStoreConfig;

public final class UserStore extends STCollectionStore<User> implements ConfigurableStore<UserStoreConfig> {

	@ConfigStruct
	public static final class UserStoreConfig {

		public STDocument collection;
	}

	private static final Logger log = LoggerFactory.getLogger(UserStore.class);

	/**
	 * The global context {@link UserStore}.
	 */
	public static final UserStore UserStore = new UserStore();

	public UserStore() {
		super(log, User::new);
	}

	/**
	 * Create a new user from the given configuration.
	 *
	 * @param config The user configuration
	 */
	public User create(UserConfig config) {
		Objects.requireNonNull(config);

		return create(user -> {
			user.set(UserOid.USERNAME, config.getUsername());
			user.set(UserOid.EMAIL, config.getEmail());
			user.set(UserOid.EXPIRATION, config.getExpiration());
			user.set(UserOid.HASH, CryptoUtil.PBKDF2.hash(
					// Compute a preliminary hash before PBKDF2 is applied
					Hashing.sha512().hashString(config.getPassword(), Charsets.UTF_8).toString()));
		});
	}

	public Optional<User> getByCvid(int cvid) {
		return null;
	}

	public Optional<User> getByUsername(String username) {
		return values().stream().filter(user -> username.equals(user.get(UserOid.USERNAME))).findAny();
	}

	@Override
	public void init(Consumer<UserStoreConfig> configurator) {
		var config = new UserStoreConfig();
		configurator.accept(config);

		setDocument(config.collection);
	}
}
