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

import static com.sandpolis.core.foundation.util.ProtoUtil.begin;
import static com.sandpolis.core.foundation.util.ProtoUtil.failure;
import static com.sandpolis.core.foundation.util.ProtoUtil.success;
import static com.sandpolis.core.instance.Metatypes.InstanceType.CLIENT;
import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.server.group.GroupStore.GroupStore;
import static com.sandpolis.core.server.user.UserStore.UserStore;

import java.util.concurrent.ExecutorService;

import com.google.protobuf.MessageLiteOrBuilder;
import com.sandpolis.core.clientserver.msg.MsgAgentbuilder.RQ_BuildAgent;
import com.sandpolis.core.clientserver.msg.MsgGroup.RQ_GroupOperation;
import com.sandpolis.core.foundation.Result.ErrorCode;
import com.sandpolis.core.net.exelet.Exelet;
import com.sandpolis.core.net.exelet.ExeletContext;
import com.sandpolis.core.server.agentbuilder.CreateAgentTask;

/**
 * {@link GroupExe} contains message handlers related to group management.
 *
 * @since 5.0.0
 */
public final class GroupExe extends Exelet {

	@Handler(auth = true, instances = CLIENT)
	public static MessageLiteOrBuilder rq_group_operation(ExeletContext context, RQ_GroupOperation rq) {
		var outcome = begin();
		var user = UserStore.getByCvid(context.connector.getRemoteCvid()).orElse(null);
		if (user == null)
			return failure(outcome, ErrorCode.ACCESS_DENIED);

		switch (rq.getOperation()) {
		case GROUP_CREATE:
			if (rq.getGroupConfigCount() != 1)
				return failure(outcome);

			GroupStore.create(rq.getGroupConfig(0));
			break;
		default:
			break;
		}

		return success(outcome);
	}

	@Handler(auth = true, instances = CLIENT)
	public static MessageLiteOrBuilder rq_build_agent(RQ_BuildAgent rq) throws Exception {
		ExecutorService pool = ThreadStore.get("server.generator");

		Group group = GroupStore.getByName(rq.getGroup()).get();

		var task = new CreateAgentTask(rq.getConfig(), rq.getGeneratorOptions(), rq.getPackagerOptions(),
				rq.getDeploymentOptions());

		return task.start().toCompletableFuture().get();
	}

	private GroupExe() {
	}
}
