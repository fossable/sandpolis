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
package com.sandpolis.server.vanilla.store.user;

import static com.google.common.base.Preconditions.checkArgument;
import static com.sandpolis.core.util.CryptoUtil.SHA256;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Result.ErrorCode;
import com.sandpolis.core.instance.User.ProtoUser;
import com.sandpolis.core.instance.storage.MemoryMapStoreProvider;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.instance.store.StoreBase.StoreConfig;
import com.sandpolis.core.util.CryptoUtil;
import com.sandpolis.server.vanilla.store.user.UserStore.UserStoreConfig;

public final class UserStore extends MapStore<String, User, UserStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(UserStore.class);

	public UserStore() {
		super(log);
	}

	/**
	 * Check a user's expiration status.
	 *
	 * @param user The user to check
	 * @return Whether the given user is currently expired
	 */
	public boolean isExpired(User user) {
		Objects.requireNonNull(user);

		return user.getExpiration() > 0 && user.getExpiration() < System.currentTimeMillis();
	}

	/**
	 * Check a user's expiration status.
	 *
	 * @param user The user to check
	 * @return Whether the given user is currently expired
	 */
	public boolean isExpired(String user) {
		Objects.requireNonNull(user);

		return isExpired(get(user).get());
	}

	/**
	 * Create a new user from the given configuration and add it to the store.
	 *
	 * @param config The user configuration
	 */
	public void add(ProtoUser.Builder config) {
		add(config.build());
	}

	/**
	 * Create a new user from the given configuration and add it to the store.
	 *
	 * @param config The user configuration
	 */
	public void add(ProtoUser config) {
		Objects.requireNonNull(config);
		checkArgument(User.valid(config) == ErrorCode.OK, "Invalid configuration");

		// Create the user
		User user = new User(config);
		user.setCreation(System.currentTimeMillis());
		user.setHash(CryptoUtil.PBKDF2.hash(CryptoUtil.hash(SHA256, config.getPassword())));

		add(user);
	}

	/**
	 * Change a user's configuration or statistics.
	 *
	 * @param id    The ID of the user to modify
	 * @param delta The changes
	 * @return The outcome of the action
	 */
	public ErrorCode delta(long id, ProtoUser delta) {
		User user = null;// get(id).orElse(null);
		if (user == null)
			return ErrorCode.UNKNOWN_USER;

		user.merge(delta);
		return ErrorCode.OK;
	}

	@Override
	public UserStore init(Consumer<UserStoreConfig> configurator) {
		var config = new UserStoreConfig();
		configurator.accept(config);

		config.defaults.forEach(this::add);

		return (UserStore) super.init(null);
	}

	public final class UserStoreConfig extends StoreConfig {

		public final List<ProtoUser> defaults = new ArrayList<>();

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(User.class, User::getUsername);
		}

		@Override
		public void persistent(Database database) {
			provider = database.getConnection().provider(User.class, "username");
		}
	}

	public static final UserStore UserStore = new UserStore();
}
