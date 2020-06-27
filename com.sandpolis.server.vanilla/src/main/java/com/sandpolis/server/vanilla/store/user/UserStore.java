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

import com.sandpolis.core.instance.User.UserConfig;
import com.sandpolis.core.instance.storage.MemoryMapStoreProvider;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.server.vanilla.store.user.UserStore.UserStoreConfig;

public final class UserStore extends MapStore<User, UserStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(UserStore.class);

	/**
	 * Create a new user from the given configuration and add it to the store.
	 *
	 * @param config The user configuration
	 */
	public void add(UserConfig config) {
		Objects.requireNonNull(config);

		add(new User(document, config));
	}

	public Optional<User> getByUsername(String username) {
		return stream().filter(user -> user.getUsername().equals(username)).findAny();
	}

	@Override
	public UserStore init(Consumer<UserStoreConfig> configurator) {
		var config = new UserStoreConfig();
		configurator.accept(config);

		config.defaults.forEach(this::add);

		return (UserStore) super.init(null);
	}

	public final class UserStoreConfig extends StoreConfig {

		public final List<UserConfig> defaults = new ArrayList<>();

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(User.class);
		}

		@Override
		public void persistent(Database database) {
			provider = database.getConnection().provider(User.class);
		}
	}

	public UserStore() {
		super(log);
	}

	/**
	 * The global context {@link UserStore}.
	 */
	public static final UserStore UserStore = new UserStore();
}
