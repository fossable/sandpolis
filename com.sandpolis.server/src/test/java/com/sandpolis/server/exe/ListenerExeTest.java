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
import com.sandpolis.core.net.init.ChannelConstant;
import com.sandpolis.core.proto.net.MCListener.RQ_AddListener;
import com.sandpolis.core.proto.net.MCListener.RQ_RemoveListener;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.pojo.Listener.ListenerConfig;
import com.sandpolis.core.proto.pojo.User.UserConfig;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.server.store.listener.Listener;
import com.sandpolis.server.store.listener.ListenerStore;
import com.sandpolis.server.store.user.User;
import com.sandpolis.server.store.user.UserStore;

class ListenerExeTest extends ExeletTest {

	private ListenerExe exe;

	@BeforeEach
	void setup() {
		initChannel();
		exe = new ListenerExe(new Sock(channel));
		channel.attr(ChannelConstant.CVID).set(90);

		UserStore.init(StoreProviderFactory.memoryList(User.class));
		UserStore.add(UserConfig.newBuilder().setUsername("junit").setPassword("12345678").build());
		UserStore.get("junit").setCvid(90);

		ListenerStore.init(StoreProviderFactory.memoryList(Listener.class));
		ListenerStore.add(ListenerConfig.newBuilder().setOwner("junit").setPort(5000).setAddress("0.0.0.0").build());
	}

	@Test
	void testDeclaration() {
		testDeclaration(ListenerExe.class);
	}

	@Test
	void testRqAddListenerValid() {
		exe.rq_add_listener(rq(RQ_AddListener.newBuilder()
				.setConfig(ListenerConfig.newBuilder().setId(2).setOwner("junit").setPort(5000).setAddress("0.0.0.0")))
						.build());

		Outcome outcome = ((Message) channel.readOutbound()).getRsOutcome();
		assertTrue(outcome.getResult(), outcome.getComment());
	}

	@Test
	void testRqAddListenerInvalidConfiguration() {
		exe.rq_add_listener(rq(RQ_AddListener.newBuilder().setConfig(ListenerConfig.newBuilder().setId(7))).build());

		Outcome outcome = ((Message) channel.readOutbound()).getRsOutcome();
		assertFalse(outcome.getResult(), outcome.getComment());
	}

	@Test
	void testRqRemoveListenerValid() {
		exe.rq_remove_listener(rq(RQ_RemoveListener.newBuilder().setId(0)).build());

		Outcome outcome = ((Message) channel.readOutbound()).getRsOutcome();
		assertTrue(outcome.getResult(), outcome.getComment());
	}

}
