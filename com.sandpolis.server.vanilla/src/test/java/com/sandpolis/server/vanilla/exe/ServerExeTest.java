/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.server.vanilla.exe;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.net.command.ExeletTest;
import com.sandpolis.core.proto.net.MsgPing.RQ_Ping;
import com.sandpolis.core.proto.net.MsgPing.RS_Ping;

class ServerExeTest extends ExeletTest {

	@BeforeEach
	void setup() {
		ThreadStore.init(config -> {
			config.ephemeral();

			config.defaults.put("store.event_bus", Executors.newSingleThreadExecutor());
		});

		initTestContext();
	}

	@Test
	void testDeclaration() {
		testNameUniqueness(ServerExe.class);
	}

	@Test
	@DisplayName("Check that pings get a response")
	void rq_ping_1() {
		var rq = RQ_Ping.newBuilder().build();
		var rs = ServerExe.rq_ping(rq);

		assertEquals(RS_Ping.newBuilder().build(), ((RS_Ping.Builder) rs).build());
	}
}
