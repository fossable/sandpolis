//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.group;

import static org.s7s.core.foundation.Instance.InstanceType.CLIENT;
import static org.s7s.core.instance.thread.ThreadStore.ThreadStore;
import static org.s7s.core.server.group.GroupStore.GroupStore;
import static org.s7s.core.server.user.UserStore.UserStore;

import java.util.concurrent.ExecutorService;

import org.s7s.core.protocol.Group.RQ_BuildAgent;
import org.s7s.core.protocol.Group.RS_BuildAgent;
import org.s7s.core.protocol.Group.RQ_CreateGroup;
import org.s7s.core.protocol.Group.RS_CreateGroup;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ConnectionOid;
import org.s7s.core.instance.state.InstanceOids.GroupOid;
import org.s7s.core.instance.exelet.Exelet;
import org.s7s.core.instance.exelet.ExeletContext;
import org.s7s.core.server.agentbuilder.CreateAgentTask;

/**
 * {@link GroupExe} contains message handlers related to group management.
 *
 * @since 5.0.0
 */
public final class GroupExe extends Exelet {

	@Handler(auth = true, instances = CLIENT)
	public static RS_CreateGroup rq_create_group(ExeletContext context, RQ_CreateGroup rq) {

		var user = UserStore.getBySid(context.connector.get(ConnectionOid.REMOTE_SID).asInt()).orElse(null);
		if (user == null)
			return RS_CreateGroup.CREATE_GROUP_FAILED_ACCESS_DENIED;

		GroupStore.create(group -> {
//			group.set(GroupOid.NAME, config.getName());
//			for (var mech : config.getPasswordMechanismList()) {
//				// TODO
//			}
//			UserStore.getByUsername(config.getOwner()).ifPresent(user -> {
//				// group.set(GroupOid.OWNER, user.oid());
//			});
		});

		return RS_CreateGroup.CREATE_GROUP_OK;
	}

	@Handler(auth = true, instances = CLIENT)
	public static RS_BuildAgent rq_build_agent(RQ_BuildAgent rq) throws Exception {
		ExecutorService pool = ThreadStore.get("server.generator");

		Group group = GroupStore.getByName(rq.getGroup()).get();

		var task = new CreateAgentTask(null /* rq.getConfig() */, rq.getGeneratorOptions(), rq.getPackagerOptions(),
				rq.getDeploymentOptions());

		return task.start().toCompletableFuture().get();
	}

	private GroupExe() {
	}
}
