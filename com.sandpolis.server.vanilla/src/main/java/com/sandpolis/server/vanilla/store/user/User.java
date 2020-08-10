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
package com.sandpolis.server.vanilla.store.user;

import java.util.List;

import javax.persistence.ManyToMany;

import com.sandpolis.core.instance.StateTree.VirtProfile.VirtServer.VirtUser;
import com.sandpolis.core.instance.state.Document;
import com.sandpolis.server.vanilla.store.group.Group;

/**
 * Represents a user account on the server.
 *
 * @author cilki
 * @since 5.0.0
 */
public class User extends VirtUser {

	@ManyToMany(mappedBy = "members")
	private List<Group> groups;

	User(Document parent) {
		super(parent);
	}

	/**
	 * Check a user's expiration status.
	 *
	 * @param user The user to check
	 * @return Whether the given user is currently expired
	 */
	public boolean isExpired() {
		var expiration = getExpiration();
		if (expiration == null)
			return false;

		return expiration.getTime() > 0 && expiration.getTime() < System.currentTimeMillis();
	}

}
