/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.server.store.group;

import static com.sandpolis.core.util.ProtoUtil.begin;
import static com.sandpolis.core.util.ProtoUtil.failure;
import static com.sandpolis.core.util.ProtoUtil.success;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Store;
import com.sandpolis.core.instance.Store.ManualInitializer;
import com.sandpolis.core.instance.storage.StoreProvider;
import com.sandpolis.core.instance.storage.StoreProviderFactory;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.proto.pojo.Group.GroupConfig;
import com.sandpolis.core.proto.pojo.Group.ProtoGroup;
import com.sandpolis.core.proto.util.Result.ErrorCode;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.util.ValidationUtil;
import com.sandpolis.server.store.user.User;

/**
 * The {@link GroupStore} manages groups and authentication mechanisms.
 * 
 * @author cilki
 * @since 5.0.0
 */
@ManualInitializer
public final class GroupStore extends Store {

	private static final Logger log = LoggerFactory.getLogger(GroupStore.class);

	private static StoreProvider<Group> provider;

	public static void init(StoreProvider<Group> provider) {
		GroupStore.provider = Objects.requireNonNull(provider);
	}

	public static void load(Database main) {
		Objects.requireNonNull(main);

		init(StoreProviderFactory.database(Group.class, main));
	}

	/**
	 * Get a group from the store.
	 * 
	 * @param groupId The ID of a group
	 * @return The requested {@link Group} or {@code null} if not found
	 */
	public static Group get(String groupId) {
		return provider.get("id", groupId);
	}

	/**
	 * Create a new group from the given configuration and add it to the store.
	 * 
	 * @param config The group configuration
	 * @return The outcome of the action
	 */
	public static Outcome add(GroupConfig config) {
		ErrorCode code = ValidationUtil.Config.valid(config);
		if (code != ErrorCode.OK)
			return Outcome.newBuilder().setResult(false).setError(code).build();
		code = ValidationUtil.Config.complete(config);
		if (code != ErrorCode.OK)
			return Outcome.newBuilder().setResult(false).setError(code).build();

		return add(new Group(config));
	}

	/**
	 * Add a group to the store.
	 * 
	 * @param group The group to add
	 * @return The outcome of the action
	 */
	public static Outcome add(Group group) {
		Outcome.Builder outcome = begin();
		if (get(group.getGroupId()) != null)
			return failure(outcome, "Group ID is already taken");

		provider.add(group);
		return success(outcome);
	}

	/**
	 * Change a group's configuration or statistics.
	 * 
	 * @param id    The ID of the group to modify
	 * @param delta The changes
	 * @return The outcome of the action
	 */
	public static Outcome delta(String id, ProtoGroup delta) {
		Outcome.Builder outcome = begin();
		Group group = get(id);
		if (group == null)
			return failure(outcome, "Group not found");

		ErrorCode error = group.merge(delta);
		if (error != ErrorCode.OK)
			return failure(outcome.setError(error));

		return success(outcome);
	}

	/**
	 * Remove a group from the store.
	 * 
	 * @param id The group's ID
	 * @return The outcome of the action
	 */
	public static Outcome remove(String id) {
		Outcome.Builder outcome = begin();
		provider.remove(get(id));
		return success(outcome);
	}

	/**
	 * Get all groups that the given user owns or is a member of.
	 * 
	 * @param user A user
	 * @return A list of the user's groups
	 */
	public static List<Group> getMembership(User user) {
		try (Stream<Group> stream = provider.stream()) {
			return stream.filter(group -> user.equals(group.getOwner()) || group.getMembers().contains(user))
					.collect(Collectors.toList());
		}
	}

	/**
	 * Get a list of all groups.
	 * 
	 * @return A list of all groups in the store
	 */
	public static List<Group> getGroups() {
		try (Stream<Group> stream = provider.stream()) {
			return stream.collect(Collectors.toList());
		}
	}

	/**
	 * Get a list of groups without an auth mechanism.
	 * 
	 * @return A list of unauth groups
	 */
	public static List<Group> getUnauthGroups() {
		try (Stream<Group> stream = provider.stream()) {
			return stream.filter(group -> group.getKeys().size() == 0 && group.getPasswords().size() == 0)
					.collect(Collectors.toList());
		}
	}

	/**
	 * Get a list of groups with the given password.
	 * 
	 * @param password The password
	 * @return A list of groups with the password
	 */
	public static List<Group> getByPassword(String password) {
		try (Stream<Group> stream = provider.stream()) {
			return stream.filter(
					group -> group.getPasswords().stream().anyMatch(mech -> mech.getPassword().equals(password)))
					.collect(Collectors.toList());
		}
	}

	private GroupStore() {
	}

}
