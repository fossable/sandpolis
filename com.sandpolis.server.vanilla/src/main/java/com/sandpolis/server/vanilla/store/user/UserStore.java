/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.server.vanilla.store.user;

import static com.google.common.base.Preconditions.checkArgument;
import static com.sandpolis.core.util.CryptoUtil.SHA256;
import static com.sandpolis.core.util.ProtoUtil.begin;
import static com.sandpolis.core.util.ProtoUtil.failure;
import static com.sandpolis.core.util.ProtoUtil.success;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Store;
import com.sandpolis.core.instance.Store.ManualInitializer;
import com.sandpolis.core.instance.storage.StoreProvider;
import com.sandpolis.core.instance.storage.StoreProviderFactory;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.proto.pojo.User.ProtoUser;
import com.sandpolis.core.proto.pojo.User.UserConfig;
import com.sandpolis.core.proto.util.Result.ErrorCode;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.util.CryptoUtil;
import com.sandpolis.core.util.ValidationUtil;

@ManualInitializer
public final class UserStore extends Store {

	private static final Logger log = LoggerFactory.getLogger(UserStore.class);

	private static StoreProvider<User> provider;

	public static void init(StoreProvider<User> provider) {
		UserStore.provider = Objects.requireNonNull(provider);

		if (log.isDebugEnabled())
			log.debug("Initialized store containing {} entities", provider.count());
	}

	public static void load(Database main) {
		init(StoreProviderFactory.database(User.class, Objects.requireNonNull(main)));
	}

	/**
	 * Check if a user exists in the store.
	 * 
	 * @param username The user's username
	 * @return Whether the given user exists
	 */
	public static boolean exists(String username) {
		Objects.requireNonNull(username);

		return get(username).isPresent();
	}

	/**
	 * Check if a user exists in the store.
	 * 
	 * @param id The user's ID
	 * @return Whether the given user exists
	 */
	public static boolean exists(long id) {
		return get(id).isPresent();
	}

	/**
	 * Check a user's expiration status.
	 * 
	 * @param user The user to check
	 * @return Whether the given user is currently expired
	 */
	public static boolean isExpired(User user) {
		Objects.requireNonNull(user);

		return user.getExpiration() > 0 && user.getExpiration() < System.currentTimeMillis();
	}

	/**
	 * Check a user's expiration status.
	 * 
	 * @param user The user to check
	 * @return Whether the given user is currently expired
	 */
	public static boolean isExpired(String user) {
		Objects.requireNonNull(user);

		return isExpired(get(user).get());
	}

	/**
	 * Get a {@link User} from the store.
	 * 
	 * @param username The username to query
	 * @return The requested user
	 */
	public static Optional<User> get(String username) {
		return provider.get("username", username);
	}

	/**
	 * Get a {@link User} from the store.
	 * 
	 * @param id The user ID to query
	 * @return The requested user
	 */
	public static Optional<User> get(long id) {
		return provider.get("id", id);
	}

	/**
	 * Remove a {@link User} from the store if it exists.
	 * 
	 * @param username The username to remove
	 */
	public static void remove(String username) {
		log.debug("Deleting user \"{}\"", username);

		get(username).ifPresent(provider::remove);
	}

	/**
	 * Remove a {@link User} from the store if it exists.
	 * 
	 * @param id The ID of the user to remove
	 */
	public static void remove(long id) {
		log.debug("Deleting user {}", id);

		get(id).ifPresent(provider::remove);
	}

	/**
	 * Create a new user from the given configuration and add it to the store.
	 * 
	 * @param config The user configuration
	 */
	public static void add(UserConfig.Builder config) {
		add(config.build());
	}

	/**
	 * Create a new user from the given configuration and add it to the store.
	 * 
	 * @param config The user configuration
	 */
	public static void add(UserConfig config) {
		Objects.requireNonNull(config);
		checkArgument(ValidationUtil.Config.valid(config) == ErrorCode.OK, "Invalid configuration");

		// Create the user
		User user = new User(config);
		user.setCreation(System.currentTimeMillis());
		user.setHash(CryptoUtil.PBKDF2.hash(CryptoUtil.hash(SHA256, config.getPassword())));

		add(user);
	}

	/**
	 * Add a user to the store.
	 * 
	 * @param user The new user
	 */
	public static void add(User user) {
		Objects.requireNonNull(user);
		checkArgument(get(user.getUsername()).isEmpty(), "Username conflict");

		log.debug("Adding new user: {}", user.getUsername());
		provider.add(user);
	}

	/**
	 * Change a user's configuration or statistics.
	 * 
	 * @param id    The ID of the user to modify
	 * @param delta The changes
	 * @return The outcome of the action
	 */
	public static Outcome delta(long id, ProtoUser delta) {
		Outcome.Builder outcome = begin();
		User user = get(id).orElse(null);
		if (user == null)
			return failure(outcome, "User not found");

		ErrorCode error = user.merge(delta);
		if (error != ErrorCode.OK)
			return failure(outcome.setError(error));

		return success(outcome);
	}

	private UserStore() {
	}
}
