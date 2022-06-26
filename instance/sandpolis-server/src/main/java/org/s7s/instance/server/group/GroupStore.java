//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.group;

import static org.s7s.core.server.user.UserStore.UserStore;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.instance.Group.GroupConfig;
import org.s7s.core.instance.state.InstanceOids.GroupOid;
import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.store.ConfigurableStore;
import org.s7s.core.instance.store.STCollectionStore;
import org.s7s.core.server.group.GroupStore.GroupStoreConfig;
import org.s7s.core.server.user.User;

/**
 * {@link GroupStore} manages authentication groups.
 *
 * @since 5.0.0
 */
public final class GroupStore extends STCollectionStore<Group> implements ConfigurableStore<GroupStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(GroupStore.class);

	public GroupStore() {
		super(log, Group::new);
	}

	/**
	 * Get all groups that the given user owns or is a member of.
	 *
	 * @param user A user
	 * @return The user's groups
	 */
	public Stream<Group> getByMembership(User user) {
		return values().stream().filter(group -> {
			var owner = group.get(GroupOid.OWNER);
			return false;// user.equals(group.getOwner()) || group.getMembers().contains(user);
		});
	}

	/**
	 * Get a list of groups without an auth mechanism.
	 *
	 * @return A list of unauth groups
	 */
	public Stream<Group> getUnauthGroups() {
		return values().stream();// .filter(group -> group.getAuthMechanisms().size() == 0);
	}

	/**
	 * Get a list of groups with the given password.
	 *
	 * @param password The password
	 * @return Groups with the password
	 */
	public Stream<Group> getByPassword(String password) {
		return values().stream().filter(group -> {
//			for (var mech : group.getAuthMechanisms()) {
//				if (password.equals(mech.getPassword())) {
//					return true;
//				}
//			}
			return false;
		});
	}

	public Optional<Group> getByName(String name) {
		return values().stream().filter(group -> name.equals(group.get(GroupOid.NAME).asString())).findAny();
	}

	@Override
	public void init(Consumer<GroupStoreConfig> configurator) {
		var config = new GroupStoreConfig(configurator);

		setDocument(config.collection);
	}

	/**
	 * Create a new group from the given configuration.
	 *
	 * @param config The group configuration
	 */
	public Group create(GroupConfig config) {
		return create(group -> {
			group.set(GroupOid.NAME, config.getName());
			for (var mech : config.getPasswordMechanismList()) {
				// TODO
			}
			UserStore.getByUsername(config.getOwner()).ifPresent(user -> {
				// group.set(GroupOid.OWNER, user.oid());
			});
		});
	}

	public static final class GroupStoreConfig {

		public STDocument collection;

		private GroupStoreConfig(Consumer<GroupStoreConfig> configurator) {
			configurator.accept(this);
		}
	}

	public static final GroupStore GroupStore = new GroupStore();
}
