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

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.sandpolis.core.foundation.util.CryptoUtil;
import com.sandpolis.core.instance.DocumentBindings.Profile;
import com.sandpolis.core.instance.User.UserConfig;
import com.sandpolis.core.instance.data.Collection;
import com.sandpolis.core.instance.data.Document;
import com.sandpolis.server.vanilla.store.group.Group;

/**
 * Represents a user account on the server.
 *
 * @author cilki
 * @since 5.0.0
 */
public class User extends Profile.Instance.Server.User {

	@ManyToMany(mappedBy = "members")
	private List<Group> groups;

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

	public User(Document parent) {
		super(parent);
	}

	/**
	 * Construct a new {@link User} from a configuration.
	 *
	 * @param config The configuration which should be prevalidated and complete
	 */
	public User(Collection parent, UserConfig config) {
		super(new Document(parent));

		hash().set(CryptoUtil.PBKDF2.hash(
				// Compute a preliminary hash before PBKDF2 is applied
				Hashing.sha512().hashString(config.getPassword(), Charsets.UTF_8).toString()));
		username().set(config.getUsername());
	}

}
