//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.user;

import static org.s7s.core.foundation.Instance.InstanceType.CLIENT;
import static org.s7s.core.server.user.UserStore.UserStore;

import org.s7s.core.protocol.User.RQ_CreateUser;
import org.s7s.core.protocol.User.RS_CreateUser;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ConnectionOid;
import org.s7s.core.instance.exelet.Exelet;
import org.s7s.core.instance.exelet.ExeletContext;

/**
 * {@link UserExe} contains message handlers related to user management.
 *
 * @since 4.0.0
 */
public final class UserExe extends Exelet {

	@Handler(auth = true, instances = CLIENT)
	public static RS_CreateUser rq_create_user(ExeletContext context, RQ_CreateUser rq) {

		var user = UserStore.getBySid(context.connector.get(ConnectionOid.REMOTE_SID).asInt()).orElse(null);
		if (user == null)
			return RS_CreateUser.CREATE_USER_ACCESS_DENIED;

		UserStore.create(config -> {
			// TODO
		});

		return RS_CreateUser.CREATE_USER_OK;
	}

	private UserExe() {
	}
}
