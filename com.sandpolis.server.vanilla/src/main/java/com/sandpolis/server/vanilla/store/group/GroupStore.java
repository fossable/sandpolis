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
package com.sandpolis.server.vanilla.store.group;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.storage.StoreProvider;
import com.sandpolis.core.instance.store.StoreBase;
import com.sandpolis.core.proto.pojo.Group.GroupConfig;
import com.sandpolis.core.proto.pojo.Group.ProtoGroup;
import com.sandpolis.core.proto.util.Result.ErrorCode;
import com.sandpolis.core.util.ValidationUtil;
import com.sandpolis.server.vanilla.store.group.GroupStore.GroupStoreConfig;
import com.sandpolis.server.vanilla.store.user.User;

/**
 * The {@link GroupStore} manages groups and authentication mechanisms.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class GroupStore extends StoreBase<GroupStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(GroupStore.class);

	private static StoreProvider<Group> provider;

	/**
	 * Get a group from the store.
	 * 
	 * @param id The ID of a group
	 * @return The requested {@link Group}
	 */
	public static Optional<Group> get(String id) {
		return provider.get("id", id);
	}

	/**
	 * Create a new group from the given configuration and add it to the store.
	 * 
	 * @param config The group configuration
	 */
	public static void add(GroupConfig.Builder config) {
		add(config.build());
	}

	/**
	 * Create a new group from the given configuration and add it to the store.
	 * 
	 * @param config The group configuration
	 */
	public static void add(GroupConfig config) {
		Objects.requireNonNull(config);
		checkArgument(ValidationUtil.Config.valid(config) == ErrorCode.OK, "Invalid configuration");
		checkArgument(ValidationUtil.Config.complete(config) == ErrorCode.OK, "Incomplete configuration");

		add(new Group(config));
	}

	/**
	 * Add a group to the store.
	 * 
	 * @param group The group to add
	 */
	public static void add(Group group) {
		checkArgument(get(group.getGroupId()).isEmpty(), "ID conflict");

		log.debug("Adding new group: {}", group.getGroupId());
		provider.add(group);
	}

	/**
	 * Remove a group from the store.
	 * 
	 * @param id The group's ID
	 */
	public static void remove(String id) {
		log.debug("Deleting group {}", id);

		get(id).ifPresent(provider::remove);
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

	/**
	 * Change a group's configuration or statistics.
	 * 
	 * @param id    The ID of the group to modify
	 * @param delta The changes
	 * @return The outcome of the action
	 */
	public static ErrorCode delta(String id, ProtoGroup delta) {
		Group group = get(id).orElse(null);
		if (group == null)
			return ErrorCode.UNKNOWN_GROUP;

		return group.merge(delta);
	}

	public static final class GroupStoreConfig {

	}

	public static final GroupStore GroupStore = new GroupStore();

	@Override
	public void init(Consumer<GroupStoreConfig> o) {
		// TODO Auto-generated method stub

	}
}
