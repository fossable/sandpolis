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

import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.server.group.GroupStore.GroupStore;
import static com.sandpolis.core.server.user.UserStore.UserStore;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.clientserver.msg.MsgGroup.RQ_GroupOperation;
import com.sandpolis.core.foundation.Result.Outcome;
import com.sandpolis.core.instance.Group.GroupConfig;
import com.sandpolis.core.instance.User.UserConfig;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.exelet.ExeletContext;

import io.netty.channel.embedded.EmbeddedChannel;

@Disabled
class GroupExeTest {

	protected ExeletContext context;

	@BeforeEach
	void setup() {
		UserStore.init(config -> {
		});
		UserStore.create(UserConfig.newBuilder().setUsername("admin").setPassword("123").build());
		GroupStore.init(config -> {
		});
		ThreadStore.init(config -> {
			config.defaults.put("store.event_bus", Executors.newSingleThreadExecutor());
		});

		context = new ExeletContext(ConnectionStore.create(new EmbeddedChannel()), MSG.newBuilder().build());
	}

	@Test
	void testDeclaration() {
	}

	@Test
	@DisplayName("Add a valid memberless group")
	void rq_add_group_1() {
		var rq = RQ_GroupOperation.newBuilder()
				.addGroupConfig(GroupConfig.newBuilder().setId("123").setName("default").setOwner("admin")).build();
		var rs = GroupExe.rq_group_operation(context, rq);

		assertTrue(((Outcome) rs).getResult());
	}

	@Test
	@DisplayName("Add a valid group with members")
	void rq_add_group_2() {
		var rq = RQ_GroupOperation.newBuilder().addGroupConfig(
				GroupConfig.newBuilder().setId("123").setName("default2").setOwner("admin").addMember("demouser"))
				.build();
		var rs = GroupExe.rq_group_operation(context, rq);

		assertTrue(((Outcome) rs).getResult());
	}

	@Test
	@DisplayName("Try to remove a missing group")
	void rq_add_group_3() {
		var rq = RQ_GroupOperation.newBuilder().addGroupConfig(GroupConfig.newBuilder().setId("9292")).build();
		var rs = GroupExe.rq_group_operation(context, rq);

		assertFalse(((Outcome) rs).getResult());
	}

}
