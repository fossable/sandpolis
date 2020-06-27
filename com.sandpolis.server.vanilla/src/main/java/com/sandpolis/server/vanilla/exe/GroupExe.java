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
import static com.sandpolis.core.instance.util.ProtoUtil.failure;
import static com.sandpolis.core.instance.util.ProtoUtil.success;
import static com.sandpolis.server.vanilla.store.group.GroupStore.GroupStore;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.Result.ErrorCode;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.MsgGroup.RQ_AddGroup;
import com.sandpolis.core.net.MsgGroup.RQ_GroupDelta;
import com.sandpolis.core.net.MsgGroup.RQ_ListGroups;
import com.sandpolis.core.net.MsgGroup.RQ_RemoveGroup;
import com.sandpolis.core.net.MsgGroup.RS_ListGroups;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.exelet.ExeletContext;

/**
 * Group message handlers.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class GroupExe extends Exelet {

	@Auth
	@Handler(tag = MSG.RQ_GROUP_OPERATION_FIELD_NUMBER)
	public static MessageOrBuilder rq_group_operation(ExeletContext context, RQ_GroupOperation rq) {
		if (!checkOwnership(context, rq.getId()))
			return failure(ErrorCode.ACCESS_DENIED);
		var outcome = begin();

		switch (rq.getOperation()) {
		case GROUP_CREATE:
			GroupStore.add(rq.getConfig());
			break;
		case GROUP_DELETE:
			GroupStore.remove(rq.getId());
			break;
		}

		return success(outcome);
	}

	private static boolean checkOwnership(ExeletContext context, String groupId) {
		Group group = GroupStore.get(groupId).orElse(null);
		if (group == null)
			return false;

		return group.getOwner().getCvid() == context.connector.getRemoteCvid();
	}

	private GroupExe() {
	}
}
