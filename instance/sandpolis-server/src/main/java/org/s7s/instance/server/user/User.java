//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.user;

import org.s7s.core.instance.state.InstanceOids.UserOid;
import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.state.vst.AbstractSTDomainObject;

/**
 * Represents a user account on the server.
 *
 * @since 5.0.0
 */
public class User extends AbstractSTDomainObject {

	User(STDocument parent) {
		super(parent);
	}

	/**
	 * Check a user's expiration status.
	 *
	 * @return Whether the given user is currently expired
	 */
	public boolean isExpired() {
		var expiration = get(UserOid.EXPIRATION);
		if (!expiration.isPresent())
			return false;

		return expiration.asLong() > 0 && expiration.asLong() < System.currentTimeMillis();
	}

}
