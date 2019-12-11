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

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static com.sandpolis.server.vanilla.store.group.GroupStore.GroupStore;
import static com.sandpolis.server.vanilla.store.user.UserStore.UserStore;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.net.command.ExeletTest;
import com.sandpolis.core.proto.net.MsgGroup.RQ_AddGroup;
import com.sandpolis.core.proto.net.MsgGroup.RQ_RemoveGroup;
import com.sandpolis.core.proto.pojo.Group.GroupConfig;
import com.sandpolis.core.proto.pojo.User.UserConfig;
import com.sandpolis.core.proto.util.Result.Outcome;

class GroupExeTest extends ExeletTest {

	@BeforeEach
	void setup() {
		UserStore.init(config -> {
			config.ephemeral();

			config.defaults.add(UserConfig.newBuilder().setUsername("admin").setPassword("123").build());
		});
		GroupStore.init(config -> {
			config.ephemeral();
		});
		ThreadStore.init(config -> {
			config.ephemeral();

			config.defaults.put("store.event_bus", Executors.newSingleThreadExecutor());
		});

		initTestContext();
	}

	@Test
	void testDeclaration() {
		testNameUniqueness(GroupExe.class);
	}

	@Test
	@DisplayName("Add a valid memberless group")
	void rq_add_group_1() {
		var rq = RQ_AddGroup.newBuilder()
				.setConfig(GroupConfig.newBuilder().setId("123").setName("default").setOwner("admin")).build();
		var rs = GroupExe.rq_add_group(rq);

		assertTrue(((Outcome) rs).getResult());
	}

	@Test
	@DisplayName("Add a valid group with members")
	void rq_add_group_2() {
		var rq = RQ_AddGroup.newBuilder().setConfig(
				GroupConfig.newBuilder().setId("123").setName("default2").setOwner("admin").addMember("demouser"))
				.build();
		var rs = GroupExe.rq_add_group(rq);

		assertTrue(((Outcome) rs).getResult());
	}

	@Test
	@DisplayName("Try to remove a missing group")
	void rq_add_group_3() {
		var rq = RQ_RemoveGroup.newBuilder().setId("9292").build();
		var rs = GroupExe.rq_remove_group(context, rq);

		assertFalse(((Outcome) rs).getResult());
	}

}
