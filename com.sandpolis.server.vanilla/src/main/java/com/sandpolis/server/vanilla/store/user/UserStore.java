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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.sandpolis.core.foundation.util.CryptoUtil;
import com.sandpolis.core.instance.StateTree.VirtProfile.VirtServer.VirtUser;
import com.sandpolis.core.instance.User.UserConfig;
import com.sandpolis.core.instance.state.Document;
import com.sandpolis.core.instance.store.CollectionStore;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.core.instance.store.provider.MemoryMapStoreProvider;
import com.sandpolis.core.instance.store.provider.StoreProviderFactory;
import com.sandpolis.server.vanilla.store.user.UserStore.UserStoreConfig;

public final class UserStore extends CollectionStore<User> implements ConfigurableStore<UserStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(UserStore.class);

	public UserStore() {
		super(log);
	}

	public Optional<User> getByUsername(String username) {
		return stream().filter(user -> user.getUsername().equals(username)).findAny();
	}

	@Override
	public void init(Consumer<UserStoreConfig> configurator) {
		var config = new UserStoreConfig();
		configurator.accept(config);

		config.defaults.forEach(this::create);

		provider.initialize();
	}

	public User create(Consumer<User> configurator) {
		return add(new User(new Document(null)), configurator);
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

	public final class UserStoreConfig extends StoreConfig {

		public final List<UserConfig> defaults = new ArrayList<>();

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(User.class, User::tag, VirtUser.COLLECTION);
		}

		@Override
		public void persistent(StoreProviderFactory factory) {
			provider = factory.supply(User.class, User::new, VirtUser.COLLECTION);
		}
	}

	/**
	 * The global context {@link UserStore}.
	 */
	public static final UserStore UserStore = new UserStore();
}
