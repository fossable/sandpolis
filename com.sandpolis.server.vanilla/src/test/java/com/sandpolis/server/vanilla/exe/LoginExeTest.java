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
import static com.sandpolis.core.profile.store.ProfileStore.ProfileStore;
import static com.sandpolis.server.vanilla.store.user.UserStore.UserStore;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.net.command.ExeletTest;
import com.sandpolis.core.proto.net.MsgLogin.RQ_Login;
import com.sandpolis.core.proto.pojo.User.UserConfig;
import com.sandpolis.core.proto.util.Result.Outcome;

class LoginExeTest extends ExeletTest {

	@BeforeEach
	void setup() {
		UserStore.init(config -> {
			config.ephemeral();
		});
		ProfileStore.init(config -> {
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
		testNameUniqueness(LoginExe.class);
	}

	@Test
	@DisplayName("Login with missing user fails")
	void rq_login_1() {
		var rq = RQ_Login.newBuilder().setUsername("user123").setPassword("pass123").build();
		var rs = LoginExe.rq_login(context, rq);

		assertFalse(((Outcome) rs).getResult());
	}

	@Test
	@DisplayName("Login with incorrect password fails")
	void rq_login_2() {
		UserStore.add(UserConfig.newBuilder().setUsername("user123").setPassword("pass123").build());
		var rq = RQ_Login.newBuilder().setUsername("user123").setPassword("pass1234").build();
		var rs = LoginExe.rq_login(context, rq);

		assertFalse(((Outcome) rs).getResult());
	}

	@Test
	@DisplayName("Login with correct credentials succeeds")
	void rq_login_3() {
		UserStore.add(UserConfig.newBuilder().setUsername("user123").setPassword("pass123").build());
		var rq = RQ_Login.newBuilder().setUsername("user123").setPassword("pass123").build();
		var rs = LoginExe.rq_login(context, rq);

		assertTrue(((Outcome) rs).getResult());
	}

	@Test
	@DisplayName("Login with an expired user fails")
	void rq_login_4() {
		UserStore.add(UserConfig.newBuilder().setUsername("user123").setPassword("pass123").setExpiration(123).build());
		var rq = RQ_Login.newBuilder().setUsername("user123").setPassword("pass123").build();
		var rs = LoginExe.rq_login(context, rq);

		assertFalse(((Outcome) rs).getResult());
	}
}
