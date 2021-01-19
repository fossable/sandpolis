//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.server.group;

import static com.sandpolis.core.foundation.Result.ErrorCode.INVALID_GROUPNAME;
import static com.sandpolis.core.foundation.Result.ErrorCode.INVALID_ID;
import static com.sandpolis.core.foundation.Result.ErrorCode.OK;
import static com.sandpolis.core.foundation.util.ValidationUtil.group;

import com.sandpolis.core.foundation.Result.ErrorCode;
import com.sandpolis.core.instance.state.GroupOid;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.state.vst.AbstractSTDomainObject;

/**
 * A {@link Group} is a collection of users that share permissions on a
 * collection of clients. A group has one owner, who has complete control over
 * the group, and any number of members.
 *
 * <p>
 * Clients are always added to a group via an {@code AuthenticationMechanism}.
 * For example, if a group has a {@code PasswordMechanism} installed, clients
 * can supply the correct password during the authentication phase to be added
 * to the group.
 *
 * @since 5.0.0
 */
public class Group extends AbstractSTDomainObject {

	Group(STDocument parent) {
		super(parent);
	}

	@Override
	public ErrorCode valid() {

		if (attribute(GroupOid.NAME).isPresent() && !group(get(GroupOid.NAME)))
			return INVALID_GROUPNAME;

		return OK;
	}

	@Override
	public ErrorCode complete() {

		if (!attribute(GroupOid.NAME).isPresent())
			return INVALID_ID;

		return OK;
	}
}
