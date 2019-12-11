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

import static com.sandpolis.core.instance.util.ProtoUtil.begin;
import static com.sandpolis.core.instance.util.ProtoUtil.complete;
import static com.sandpolis.core.instance.util.ProtoUtil.failure;
import static com.sandpolis.core.instance.util.ProtoUtil.success;
import static com.sandpolis.core.proto.util.Result.ErrorCode.ACCESS_DENIED;
import static com.sandpolis.server.vanilla.store.user.UserStore.UserStore;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.exelet.ExeletContext;
import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.proto.net.MsgUser.RQ_AddUser;
import com.sandpolis.core.proto.net.MsgUser.RQ_RemoveUser;
import com.sandpolis.core.proto.net.MsgUser.RQ_UserDelta;
import com.sandpolis.server.vanilla.store.user.User;

/**
 * User message handlers.
 *
 * @author cilki
 * @since 4.0.0
 */
public final class UserExe extends Exelet {

	@Auth
	@Permission(permission = 0/* server.user.create */)
	@Handler(tag = MSG.RQ_ADD_USER_FIELD_NUMBER)
	public static MessageOrBuilder rq_add_user(RQ_AddUser rq) {
		var outcome = begin();

		UserStore.add(rq.getConfig());
		return success(outcome);
	}

	@Auth
	@Handler(tag = MSG.RQ_REMOVE_USER_FIELD_NUMBER)
	public static MessageOrBuilder rq_remove_user(ExeletContext context, RQ_RemoveUser rq) {
		if (!checkOwnership(context, rq.getId()))
			return failure(ACCESS_DENIED);
		var outcome = begin();

		// TODO
		// UserStore.remove(rq.getId());
		return success(outcome);
	}

	@Auth
	@Handler(tag = MSG.RQ_USER_DELTA_FIELD_NUMBER)
	public static MessageOrBuilder rq_user_delta(ExeletContext context, RQ_UserDelta rq) {
		if (!checkOwnership(context, rq.getDelta().getConfig().getId()))
			return failure(ACCESS_DENIED);
		var outcome = begin();

		return complete(outcome, UserStore.delta(rq.getId(), rq.getDelta()));
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
