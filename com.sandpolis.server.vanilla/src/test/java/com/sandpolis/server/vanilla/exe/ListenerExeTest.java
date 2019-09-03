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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.storage.StoreProviderFactory;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.command.ExeletTest;
import com.sandpolis.core.proto.net.MCListener.RQ_AddListener;
import com.sandpolis.core.proto.pojo.Listener.ListenerConfig;
import com.sandpolis.core.proto.pojo.User.UserConfig;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.server.vanilla.store.listener.Listener;
import com.sandpolis.server.vanilla.store.listener.ListenerStore;
import com.sandpolis.server.vanilla.store.user.User;
import com.sandpolis.server.vanilla.store.user.UserStore;

class ListenerExeTest extends ExeletTest {

	private ListenerExe exe;

	@BeforeEach
	void setup() {
		UserStore.init(StoreProviderFactory.memoryList(User.class));
		UserStore.add(UserConfig.newBuilder().setUsername("junit").setPassword("12345678").build());
		UserStore.get("junit").get().setCvid(90);

		ListenerStore.init(StoreProviderFactory.memoryList(Listener.class));
		ListenerStore.add(ListenerConfig.newBuilder().setOwner("junit").setPort(5000).setAddress("0.0.0.0").build());

		initChannel();
		exe = new ListenerExe();
		exe.setConnector(new Sock(channel));
	}

	@Test
	void testDeclaration() {
		testDeclaration(ListenerExe.class);
	}

	@Test
	@DisplayName("Add a listener with a valid configuration")
	void rq_add_listener_1() {
		var rs = (Outcome.Builder) exe.rq_add_listener(RQ_AddListener.newBuilder()
				.setConfig(ListenerConfig.newBuilder().setId(2).setOwner("junit").setPort(5000).setAddress("0.0.0.0"))
				.build());

		assertTrue(rs.getResult());
	}

}
