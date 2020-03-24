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
import com.sandpolis.server.vanilla.store.group.Group;

/**
 * Group message handlers.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class GroupExe extends Exelet {

	@Auth
	@Permission(permission = 0/* server.group.create */)
	@Handler(tag = MSG.RQ_ADD_GROUP_FIELD_NUMBER)
	public static MessageOrBuilder rq_add_group(RQ_AddGroup rq) {
		var outcome = begin();

		GroupStore.add(rq.getConfig());
		return success(outcome);
	}

	@Auth
	@Handler(tag = MSG.RQ_REMOVE_GROUP_FIELD_NUMBER)
	public static MessageOrBuilder rq_remove_group(ExeletContext context, RQ_RemoveGroup rq) {
		if (!checkOwnership(context, rq.getId()))
			return failure(ErrorCode.ACCESS_DENIED);
		var outcome = begin();

		GroupStore.remove(rq.getId());
		return success(outcome);
	}

	@Auth
	@Permission(permission = 0/* server.group.view */)
	@Handler(tag = MSG.RQ_LIST_GROUPS_FIELD_NUMBER)
	public static MessageOrBuilder rq_list_groups(RQ_ListGroups rq) {
		var rs = RS_ListGroups.newBuilder();

		// TODO get correct user
		GroupStore.getMembership(null).stream().map(group -> group.extract()).forEach(rs::addGroup);
		return rs;
	}

	@Auth
	@Handler(tag = MSG.RQ_GROUP_DELTA_FIELD_NUMBER)
	public static MessageOrBuilder rq_group_delta(ExeletContext context, RQ_GroupDelta rq) {
		if (!checkOwnership(context, rq.getDelta().getConfig().getId()))
			return failure(ErrorCode.ACCESS_DENIED);
		var outcome = begin();

		return complete(outcome, GroupStore.delta(rq.getId(), rq.getDelta()));
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
