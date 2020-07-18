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
package com.sandpolis.server.vanilla.store.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Group.GroupConfig;
import com.sandpolis.core.instance.store.MemoryMapStoreProvider;
import com.sandpolis.core.instance.database.Database;
import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.server.vanilla.store.group.GroupStore.GroupStoreConfig;
import com.sandpolis.server.vanilla.store.user.User;

/**
 * The {@link GroupStore} manages groups and authentication mechanisms.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class GroupStore extends MapStore<Group, GroupStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(GroupStore.class);

	public GroupStore() {
		super(log);
	}

	/**
	 * Create a new group from the given configuration and add it to the store.
	 *
	 * @param config The group configuration
	 */
	public void add(GroupConfig config) {
		Objects.requireNonNull(config);

		add(new Group(document, config));
	}

	/**
	 * Get all groups that the given user owns or is a member of.
	 *
	 * @param user A user
	 * @return The user's groups
	 */
	public Stream<Group> getByMembership(User user) {
		return stream().filter(group -> user.equals(group.getOwner()) || group.getMembers().contains(user));
	}

	/**
	 * Get a list of groups without an auth mechanism.
	 *
	 * @return A list of unauth groups
	 */
	public Stream<Group> getUnauthGroups() {
		return stream().filter(group -> group.getKeys().size() == 0 && group.getPasswords().size() == 0);
	}

	/**
	 * Get a list of groups with the given password.
	 *
	 * @param password The password
	 * @return Groups with the password
	 */
	public Stream<Group> getByPassword(String password) {
		return stream()
				.filter(group -> group.getPasswords().stream().anyMatch(mech -> mech.getPassword().equals(password)));
	}

	public Optional<Group> getByName(String name) {
		return stream().filter(group -> group.getName().equals(name)).findAny();
	}

	@Override
	public GroupStore init(Consumer<GroupStoreConfig> configurator) {
		var config = new GroupStoreConfig();
		configurator.accept(config);

		config.defaults.forEach(this::add);

		return (GroupStore) super.init(null);
	}

	public final class GroupStoreConfig extends StoreConfig {

		public final List<GroupConfig> defaults = new ArrayList<>();

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<String, Group>(Group.class);
		}

		@Override
		public void persistent(Database database) {
			provider = database.getConnection().provider(Group.class);
		}
	}

	public static final GroupStore GroupStore = new GroupStore();
}
