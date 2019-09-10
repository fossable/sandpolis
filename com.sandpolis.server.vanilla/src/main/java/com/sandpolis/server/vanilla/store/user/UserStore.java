/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.server.vanilla.store.user;

import static com.google.common.base.Preconditions.checkArgument;
import static com.sandpolis.core.util.CryptoUtil.SHA256;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.storage.MemoryMapStoreProvider;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.instance.store.StoreBase.StoreConfig;
import com.sandpolis.core.proto.pojo.User.ProtoUser;
import com.sandpolis.core.proto.pojo.User.UserConfig;
import com.sandpolis.core.proto.util.Result.ErrorCode;
import com.sandpolis.core.util.CryptoUtil;
import com.sandpolis.core.util.ValidationUtil;
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
	public void add(UserConfig.Builder config) {
		add(config.build());
	}

	/**
	 * Create a new user from the given configuration and add it to the store.
	 *
	 * @param config The user configuration
	 */
	public void add(UserConfig config) {
		Objects.requireNonNull(config);
		checkArgument(ValidationUtil.Config.valid(config) == ErrorCode.OK, "Invalid configuration");

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

		return user.merge(delta);
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
			provider = new MemoryMapStoreProvider<>(User.class, User::getUsername);
		}

		@Override
		public void persistent(Database database) {
			provider = database.getConnection().provider(User.class, "username");
		}
	}

	public static final UserStore UserStore = new UserStore();
}
