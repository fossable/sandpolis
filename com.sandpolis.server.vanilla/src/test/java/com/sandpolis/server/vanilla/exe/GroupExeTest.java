/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.server.vanilla.exe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.Signaler;
import com.sandpolis.core.instance.storage.StoreProviderFactory;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.command.ExeletTest;
import com.sandpolis.core.proto.net.MCGroup.RQ_AddGroup;
import com.sandpolis.core.proto.net.MCGroup.RQ_RemoveGroup;
import com.sandpolis.core.proto.pojo.Group.GroupConfig;
import com.sandpolis.core.proto.pojo.User.UserConfig;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.server.vanilla.store.group.Group;
import com.sandpolis.server.vanilla.store.group.GroupStore;
import com.sandpolis.server.vanilla.store.user.User;
import com.sandpolis.server.vanilla.store.user.UserStore;

class GroupExeTest extends ExeletTest {

	private GroupExe exe;

	@BeforeEach
	void setup() {
		UserStore.init(StoreProviderFactory.memoryList(User.class));
		UserStore.add(UserConfig.newBuilder().setUsername("admin").setPassword("123"));
		GroupStore.init(StoreProviderFactory.memoryList(Group.class));
		Signaler.init(Executors.newSingleThreadExecutor());

		initChannel();
		exe = new GroupExe();
		exe.setConnector(new Sock(channel));
	}

	@Test
	void testDeclaration() {
		testDeclaration(GroupExe.class);
	}

	@Test
	@DisplayName("Add a valid memberless group")
	void rq_add_group_1() {
		var rs = (Outcome.Builder) exe.rq_add_group(RQ_AddGroup.newBuilder()
				.setConfig(GroupConfig.newBuilder().setId("123").setName("default").setOwner("admin")).build());

		assertTrue(rs.getResult());
	}

	@Test
	@DisplayName("Add a valid group with members")
	void rq_add_group_2() {
		var rs = (Outcome.Builder) exe.rq_add_group(RQ_AddGroup.newBuilder().setConfig(
				GroupConfig.newBuilder().setId("123").setName("default2").setOwner("admin").addMember("demouser"))
				.build());

		assertTrue(rs.getResult());
	}

	@Test
	@DisplayName("Try to remove a missing group")
	void rq_add_group_3() {
		var rs = (Outcome.Builder) exe.rq_remove_group(RQ_RemoveGroup.newBuilder().setId("9292").build());

		assertFalse(rs.getResult());
	}

}
