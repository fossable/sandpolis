//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.user;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import org.s7s.core.foundation.S7SPassword;
import org.s7s.core.instance.User.UserConfig;
import org.s7s.core.instance.state.InstanceOids.UserOid;
import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.store.ConfigurableStore;
import org.s7s.core.instance.store.STCollectionStore;
import org.s7s.core.server.user.UserStore.UserStoreConfig;

public final class UserStore extends STCollectionStore<User> implements ConfigurableStore<UserStoreConfig> {

	public static final class UserStoreConfig {

		public STDocument collection;

		private UserStoreConfig(Consumer<UserStoreConfig> configurator) {
			configurator.accept(this);
		}
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
			user.set(UserOid.HASH, S7SPassword.of(
					// Compute a preliminary hash before PBKDF2 is applied
					Hashing.sha512().hashString(config.getPassword(), Charsets.UTF_8).toString()).hashPBKDF2());
		});
	}

	public Optional<User> getBySid(int sid) {
		return values().stream().filter(user -> {
			for (int c : user.get(UserOid.CURRENT_SID).asIntArray()) {
				if (c == sid) {
					return true;
				}
			}
			return false;
		}).findAny();
	}

	public Optional<User> getByUsername(String username) {
		return values().stream().filter(user -> username.equals(user.get(UserOid.USERNAME).asString())).findAny();
	}

	@Override
	public void init(Consumer<UserStoreConfig> configurator) {
		var config = new UserStoreConfig(configurator);

		setDocument(config.collection);
	}
}
