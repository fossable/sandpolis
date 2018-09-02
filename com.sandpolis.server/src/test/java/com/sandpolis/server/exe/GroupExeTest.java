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
package com.sandpolis.server.exe;

import static com.sandpolis.core.util.ProtoUtil.rq;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.storage.StoreProviderFactory;
import com.sandpolis.core.net.ExeletTest;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.proto.net.MCGroup.RQ_AddGroup;
import com.sandpolis.core.proto.net.MCGroup.RQ_RemoveGroup;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.pojo.Group.GroupConfig;
import com.sandpolis.server.store.group.Group;
import com.sandpolis.server.store.group.GroupStore;
import com.sandpolis.server.store.user.User;
import com.sandpolis.server.store.user.UserStore;

class GroupExeTest extends ExeletTest {

	private GroupExe exe;

	@BeforeEach
	void setup() {
		initChannel();
		exe = new GroupExe(new Sock(channel));

		UserStore.init(StoreProviderFactory.memoryList(User.class));
		GroupStore.init(StoreProviderFactory.memoryList(Group.class));
	}

	@Test
	void testDeclaration() {
		testDeclaration(GroupExe.class);
	}

	@Test
	void rqAddGroupEmptyMessage(Message m) {
		// Empty message
		exe.rq_add_group(rq(RQ_AddGroup.newBuilder()).build());
		assertFalse(((Message) channel.readOutbound()).getRsOutcome().getResult());
	}

	@Test
	void rqAddGroupMissingOwner(Message m) {
		// Missing owner
		exe.rq_add_group(rq(RQ_AddGroup.newBuilder().setConfig(GroupConfig.newBuilder().setName("default"))).build());
		assertFalse(((Message) channel.readOutbound()).getRsOutcome().getResult());
	}

	@Test
	void rqAddGroupMissingName(Message m) {
		// Missing name
		exe.rq_add_group(rq(RQ_AddGroup.newBuilder().setConfig(GroupConfig.newBuilder().setOwner("admin"))).build());
		assertFalse(((Message) channel.readOutbound()).getRsOutcome().getResult());
	}

	@Test
	void rqAddGroupWithoutMembers(Message m) {
		// Valid without members
		exe.rq_add_group(
				rq(RQ_AddGroup.newBuilder().setConfig(GroupConfig.newBuilder().setName("default").setOwner("admin")))
						.build());
		assertTrue(((Message) channel.readOutbound()).getRsOutcome().getResult());
	}

	@Test
	void rqAddGroupWithConflict(Message m) {
		// Group already exists
		exe.rq_add_group(
				rq(RQ_AddGroup.newBuilder().setConfig(GroupConfig.newBuilder().setName("default").setOwner("admin")))
						.build());
		assertFalse(((Message) channel.readOutbound()).getRsOutcome().getResult());
	}

	@Test
	void rqAddGroupValid(Message m) {
		// Completely valid
		exe.rq_add_group(rq(RQ_AddGroup.newBuilder()
				.setConfig(GroupConfig.newBuilder().setName("default2").setOwner("admin").addMember("demo"))).build());
		assertTrue(((Message) channel.readOutbound()).getRsOutcome().getResult());
	}

	@Test
	void rqRemoveGroupEmptyMessage(Message m) {
		// Empty message
		exe.rq_remove_group(rq(RQ_RemoveGroup.newBuilder()).build());
		assertFalse(((Message) channel.readOutbound()).getRsOutcome().getResult());
	}

	@Test
	void rqRemoveGroupMissing(Message m) {
		// Group does not exist
		exe.rq_remove_group(rq(RQ_RemoveGroup.newBuilder().setId(9292)).build());
		assertFalse(((Message) channel.readOutbound()).getRsOutcome().getResult());
	}

}
