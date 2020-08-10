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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Group.GroupConfig;
import com.sandpolis.core.instance.state.Document;
import com.sandpolis.core.instance.store.CollectionStore;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.core.instance.store.provider.MemoryMapStoreProvider;
import com.sandpolis.core.instance.store.provider.StoreProviderFactory;
import com.sandpolis.server.vanilla.store.group.GroupStore.GroupStoreConfig;
import com.sandpolis.server.vanilla.store.user.User;

/**
 * The {@link GroupStore} manages groups and authentication mechanisms.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class GroupStore extends CollectionStore<Group, GroupStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(GroupStore.class);

	public GroupStore() {
		super(log);
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
	public void init(Consumer<GroupStoreConfig> configurator) {
		var config = new GroupStoreConfig();
		configurator.accept(config);

		config.defaults.forEach(this::create);

		provider.initialize();
	}

	@Override
	public Group create(Consumer<Group> configurator) {
		var group = new Group(new Document(provider.getCollection()));
		configurator.accept(group);
		provider.add(group);
		return group;
	}

	/**
	 * Create a new group from the given configuration.
	 *
	 * @param config The group configuration
	 */
	public Group create(GroupConfig config) {
		return create(group -> {
			group.name().set(config.getName());
			// ...
		});
	}

	public final class GroupStoreConfig extends StoreConfig {

		public final List<GroupConfig> defaults = new ArrayList<>();

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(Group.class, Group::tag);
		}

		@Override
		public void persistent(StoreProviderFactory factory) {
			provider = factory.supply(Group.class, Group::new);
		}
	}

	public static final GroupStore GroupStore = new GroupStore();
}
