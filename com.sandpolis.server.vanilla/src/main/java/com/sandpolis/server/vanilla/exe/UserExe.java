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
package com.sandpolis.server.vanilla.exe;

import static com.sandpolis.core.foundation.util.ProtoUtil.begin;
import static com.sandpolis.core.foundation.util.ProtoUtil.failure;
import static com.sandpolis.core.foundation.util.ProtoUtil.success;
import static com.sandpolis.server.vanilla.store.user.UserStore.UserStore;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.foundation.Result.ErrorCode;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.exelet.ExeletContext;
import com.sandpolis.core.sv.msg.MsgUser.RQ_UserOperation;
import com.sandpolis.server.vanilla.store.user.User;

/**
 * User message handlers.
 *
 * @author cilki
 * @since 4.0.0
 */
public final class UserExe extends Exelet {

	@Handler(auth = true)
	public static MessageOrBuilder rq_user_operation(ExeletContext context, RQ_UserOperation rq) {
		if (!checkOwnership(context, rq.getId()))
			return failure(ErrorCode.ACCESS_DENIED);
		var outcome = begin();

		switch (rq.getOperation()) {
		case USER_CREATE:
			UserStore.add(rq.getConfig());
			break;
		case USER_DELETE:
			UserStore.remove(rq.getId());
			break;
		}

		return success(outcome);
	}

	private static boolean checkOwnership(ExeletContext context, long userId) {
		User user = null;// UserStore.get(userId).orElse(null);
		if (user == null)
			return false;

		return user.getCvid() == context.connector.getRemoteCvid();
	}

	private UserExe() {
	}
}
