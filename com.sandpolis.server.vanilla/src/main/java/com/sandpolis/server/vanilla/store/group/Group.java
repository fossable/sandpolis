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

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import com.sandpolis.core.instance.StateTree.VirtProfile.VirtServer.VirtGroup;
import com.sandpolis.core.instance.state.Document;
import com.sandpolis.server.vanilla.store.user.User;

/**
 * A {@link Group} is a collection of users that share permissions on a
 * collection of clients. A group has one owner, who has complete control over
 * the group, and any number of members.<br>
 * <br>
 * Clients are always added to a group via an {@code AuthenticationMechanism}.
 * For example, if a group has a {@code PasswordMechanism} installed, clients
 * can supply the correct password during the authentication phase to be added
 * to the group.
 *
 * @author cilki
 * @since 5.0.0
 */
public class Group extends VirtGroup {

	/**
	 * The group's owner.
	 */
	@ManyToOne(optional = false)
	@JoinColumn(referencedColumnName = "db_id")
	private User owner;

	/**
	 * The group's members.
	 */
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinTable(name = "user_group", joinColumns = @JoinColumn(name = "group_id", referencedColumnName = "db_id"), inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "db_id"))
	private Set<User> members;

	/**
	 * The group's password authentication mechanisms.
	 */
	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<PasswordMechanism> passwords;

	/**
	 * The group's key authentication mechanisms.
	 */
	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<KeyMechanism> keys;

	Group(Document parent) {
		super(parent);
	}
}
