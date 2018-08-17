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

import static com.sandpolis.core.util.ProtoUtil.begin;
import static com.sandpolis.core.util.ProtoUtil.failure;
import static com.sandpolis.core.util.ProtoUtil.success;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Store;
import com.sandpolis.core.instance.Store.ManualInitializer;
import com.sandpolis.core.instance.storage.StoreProvider;
import com.sandpolis.core.instance.storage.StoreProviderFactory;
import com.sandpolis.core.instance.storage.database.Database;
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
		User user = get(username);

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
	 * Create a new {@link User} and add it to the store.
	 * 
	 * @param username   The user's username
	 * @param password   The user's password
	 * @param expiration An expiration timestamp or 0 for no expiration
	 * @return The outcome of the add operation
	 */
	public static Outcome add(String username, String password, long expiration) {
		Outcome.Builder outcome = begin();
		if (!ValidationUtil.username(username))
			return failure(outcome, "Invalid username");
		if (!ValidationUtil.password(password))
			return failure(outcome, "Invalid password");
		if (exists(username))
			return failure(outcome, "User already exists");

		// Create the user
		User user = new User().setUsername(username).setCreation(System.currentTimeMillis()).setExpiration(expiration);

		// Hash password
		user.setHash(CryptoUtil.PBKDF2.hash(password));

		// Save the user
		provider.add(user);

		log.debug("User \"{}\" has been added", username);
		return success(outcome);
	}

	/**
	 * Get a {@link User} from the store.
	 * 
	 * @param username The username to query
	 * @return The user or {@code null}
	 */
	public static User get(String username) {
		return provider.get("username", username);
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

		provider.remove(get(username));

		log.debug("User \"{}\" has been deleted", username);
		return success(outcome);
	}
}
