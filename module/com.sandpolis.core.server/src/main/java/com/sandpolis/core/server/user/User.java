//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.server.user;

import static com.sandpolis.core.foundation.Result.ErrorCode.INVALID_EMAIL;
import static com.sandpolis.core.foundation.Result.ErrorCode.INVALID_USERNAME;
import static com.sandpolis.core.foundation.Result.ErrorCode.OK;

import com.sandpolis.core.foundation.Result.ErrorCode;
import com.sandpolis.core.foundation.util.ValidationUtil;
import com.sandpolis.core.instance.state.UserOid;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.state.vst.AbstractSTDomainObject;

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
		var expiration = attribute(UserOid.EXPIRATION);
		if (!expiration.isPresent())
			return false;

		return expiration.get() > 0 && expiration.get() < System.currentTimeMillis();
	}

	@Override
	public ErrorCode valid() {

		if (username().isPresent() && !ValidationUtil.username(getUsername()))
			return INVALID_USERNAME;
		if (email().isPresent() && !ValidationUtil.email(getEmail()))
			return INVALID_EMAIL;

		return OK;
	}

	@Override
	public ErrorCode complete() {

		if (!username().isPresent())
			return INVALID_USERNAME;

		return OK;
	}
}
