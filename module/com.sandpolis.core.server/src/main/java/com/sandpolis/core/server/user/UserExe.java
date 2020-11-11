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
package com.sandpolis.core.server.user;

import static com.sandpolis.core.foundation.util.ProtoUtil.begin;
import static com.sandpolis.core.foundation.util.ProtoUtil.failure;
import static com.sandpolis.core.foundation.util.ProtoUtil.success;
import static com.sandpolis.core.instance.Metatypes.InstanceType.CLIENT;
import static com.sandpolis.core.server.user.UserStore.UserStore;

import com.google.protobuf.MessageLiteOrBuilder;
import com.sandpolis.core.foundation.Result.ErrorCode;
import com.sandpolis.core.net.exelet.Exelet;
import com.sandpolis.core.net.exelet.ExeletContext;
import com.sandpolis.core.clientserver.msg.MsgUser.RQ_UserOperation;

/**
 * {@link UserExe} contains message handlers related to user management.
 *
 * @since 4.0.0
 */
public final class UserExe extends Exelet {

	@Handler(auth = true, instances = CLIENT)
	public static MessageLiteOrBuilder rq_user_operation(ExeletContext context, RQ_UserOperation rq) {
		var outcome = begin();
		var user = UserStore.getByCvid(context.connector.getRemoteCvid()).orElse(null);
		if (user == null)
			return failure(outcome, ErrorCode.ACCESS_DENIED);

		switch (rq.getOperation()) {
		case USER_CREATE:
			if (rq.getUserConfigCount() != 1)
				return failure(outcome);

			UserStore.create(rq.getUserConfig(0));
			break;
		default:
			break;
		}

		return success(outcome);
	}

	private UserExe() {
	}
}
