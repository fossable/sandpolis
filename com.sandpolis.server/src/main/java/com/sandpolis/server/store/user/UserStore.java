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
package com.sandpolis.server.store.user;

import static com.sandpolis.core.util.CryptoUtil.SHA256;
import static com.sandpolis.core.util.ProtoUtil.begin;
import static com.sandpolis.core.util.ProtoUtil.failure;
import static com.sandpolis.core.util.ProtoUtil.success;

import java.util.Date;
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
	private UserStore() {
	}

	private static final Logger log = LoggerFactory.getLogger(UserStore.class);

	private static StoreProvider<User> provider;

	public static void init(StoreProvider<User> provider) {
		if (provider == null)
			throw new IllegalArgumentException();

		UserStore.provider = provider;
	}

	public static void load(Database main) {
		if (main == null)
			throw new IllegalArgumentException();

		init(StoreProviderFactory.database(User.class, main));
	}

	/**
	 * Check if a user exists in the store.
	 * 
	 * @param username The username to check
	 * @return True if the user exists, false otherwise
	 */
	public static boolean exists(String username) {
		if (username == null)
			throw new IllegalArgumentException();

		return get(username) != null;
	}

	/**
	 * Check if a user exists in the store.
	 * 
	 * @param id The ID to check
	 * @return True if the user exists, false otherwise
	 */
	public static boolean exists(long id) {
		return get(id) != null;
	}

	/**
	 * Verify a login attempt.
	 * 
	 * @param username The username
	 * @param password The password
	 * @return The outcome of the validation operation
	 */
	public static Outcome validLogin(String username, String password) {
		if (username == null)
			throw new IllegalArgumentException();
		if (password == null)
			throw new IllegalArgumentException();

		Outcome.Builder outcome = begin("Verify login");
		if (!exists(username))
			return failure(outcome, "User does not exist");

		// Retrieve user
		User user = get(username).get();

		// Check expiration date
		if (user.getExpiration() > 0 && user.getExpiration() < System.currentTimeMillis())
			return failure(outcome, "User expired on: " + new Date(user.getExpiration()).toString());

		// Make decision
		if (CryptoUtil.PBKDF2.check(password, user.getHash())) {
			log.debug("Verified login request for user: {}", username);
			return success(outcome);
		} else {
			log.debug("Login verification failed for user: {}", username);
			return failure(outcome, "Wrong password");
		}
	}

	/**
	 * Create a new user from the given configuration and add it to the store.
	 * 
	 * @param config The user configuration
	 * @return The outcome of the action
	 */
	public static Outcome add(UserConfig config) {
		ErrorCode code = ValidationUtil.Config.valid(config);
		if (code != ErrorCode.OK)
			return Outcome.newBuilder().setResult(false).setError(code).build();
		code = ValidationUtil.Config.complete(config);
		if (code != ErrorCode.OK)
			return Outcome.newBuilder().setResult(false).setError(code).build();

		// Create the user
		User user = new User(config);

		// Set creation time
		user.setCreation(System.currentTimeMillis());

		// Hash password
		user.setHash(CryptoUtil.PBKDF2.hash(CryptoUtil.hash(SHA256, config.getPassword())));

		return add(user);
	}

	/**
	 * Add a user to the store.
	 * 
	 * @param user The user to add
	 * @return The outcome of the action
	 */
	public static Outcome add(User user) {
		Outcome.Builder outcome = begin();
		if (get(user.getUsername()) != null)
			return failure(outcome, "Username is already taken");

		provider.add(user);
		return success(outcome);
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

	/**
	 * Get a {@link User} from the store.
	 * 
	 * @param username The username to query
	 * @return The user or {@code null}
	 */
	public static Optional<User> get(String username) {
		return provider.get("username", username);
	}

	/**
	 * Get a {@link User} from the store.
	 * 
	 * @param id The ID to query
	 * @return The user or {@code null}
	 */
	public static Optional<User> get(long id) {
		return provider.get("id", id);
	}

	/**
	 * Remove a {@link User} from the store.
	 * 
	 * @param username The username to remove
	 * @return The outcome of the remove operation
	 */
	public static Outcome remove(String username) {
		Outcome.Builder outcome = begin();
		if (!exists(username))
			return failure(outcome, "User does not exist");

		get(username).ifPresent(provider::remove);

		log.debug("User \"{}\" has been deleted", username);
		return success(outcome);
	}

	/**
	 * Remove a {@link User} from the store.
	 * 
	 * @param id The ID of the user to remove
	 * @return The outcome of the remove operation
	 */
	public static Outcome remove(long id) {
		Outcome.Builder outcome = begin();
		if (!exists(id))
			return failure(outcome, "User does not exist");

		get(id).ifPresent(provider::remove);

		log.debug("User \"{}\" has been deleted", id);
		return success(outcome);
	}
}
