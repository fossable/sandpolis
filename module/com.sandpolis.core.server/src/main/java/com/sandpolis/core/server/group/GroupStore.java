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
package com.sandpolis.core.server.group;

import static com.sandpolis.core.server.user.UserStore.UserStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Group.GroupConfig;
import com.sandpolis.core.instance.StateTree.VirtProfile.VirtServer.VirtGroup;
import com.sandpolis.core.instance.state.Document;
import com.sandpolis.core.instance.store.CollectionStore;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.core.instance.store.provider.MemoryMapStoreProvider;
import com.sandpolis.core.instance.store.provider.StoreProviderFactory;
import com.sandpolis.core.server.group.GroupStore.GroupStoreConfig;
import com.sandpolis.core.server.user.User;

/**
 * {@link GroupStore} manages authentication groups.
 *
 * @since 5.0.0
 */
public final class GroupStore extends CollectionStore<Group> implements ConfigurableStore<GroupStoreConfig> {

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
		return stream().filter(group -> group.getAuthMechanisms().size() == 0);
	}

	/**
	 * Get a list of groups with the given password.
	 *
	 * @param password The password
	 * @return Groups with the password
	 */
	public Stream<Group> getByPassword(String password) {
		return stream().filter(group -> {
			for (var mech : group.getAuthMechanisms()) {
				if (password.equals(mech.getPassword())) {
					return true;
				}
			}
			return false;
		});
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

	public Group create(Consumer<Group> configurator) {
		return add(new Group(new Document(null)), configurator);
	}

	/**
	 * Create a new group from the given configuration.
	 *
	 * @param config The group configuration
	 */
	public Group create(GroupConfig config) {
		return create(group -> {
			group.name().set(config.getName());
			UserStore.getByUsername(config.getOwner()).ifPresent(user -> {
				group.setOwner(user);
			});
		});
	}

	public final class GroupStoreConfig extends StoreConfig {

		public final List<GroupConfig> defaults = new ArrayList<>();

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(Group.class, Group::tag, VirtGroup.COLLECTION);
		}

		@Override
		public void persistent(StoreProviderFactory factory) {
			provider = factory.supply(Group.class, Group::new, VirtGroup.COLLECTION);
		}
	}

	public static final GroupStore GroupStore = new GroupStore();
}
