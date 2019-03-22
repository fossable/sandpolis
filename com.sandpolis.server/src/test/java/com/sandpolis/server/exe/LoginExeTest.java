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

import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.Signaler;
import com.sandpolis.core.instance.storage.StoreProviderFactory;
import com.sandpolis.core.net.ExeletTest;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.proto.net.MCLogin.RQ_Login;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.pojo.User.UserConfig;
import com.sandpolis.server.store.user.User;
import com.sandpolis.server.store.user.UserStore;

class LoginExeTest extends ExeletTest {

	private LoginExe exe;

	@BeforeEach
	void setup() {
		UserStore.init(StoreProviderFactory.memoryList(User.class));
		Signaler.init(Executors.newSingleThreadExecutor());

		initChannel();
		exe = new LoginExe(new Sock(channel));
	}

	@Test
	void testDeclaration() {
		testDeclaration(LoginExe.class);
	}

	@Test
	@DisplayName("Login with missing user fails")
	void rq_login_1() {
		exe.rq_login(rq().setRqLogin(RQ_Login.newBuilder().setUsername("user123").setPassword("pass123")).build());

		assertFalse(((Message) channel.readOutbound()).getRsOutcome().getResult());
	}

	@Test
	@DisplayName("Login with incorrect password fails")
	void rq_login_2() {
		UserStore.add(UserConfig.newBuilder().setUsername("user123").setPassword("pass123").build());
		exe.rq_login(rq().setRqLogin(RQ_Login.newBuilder().setUsername("user123").setPassword("pass1234")).build());

		assertFalse(((Message) channel.readOutbound()).getRsOutcome().getResult());
	}

	@Test
	@DisplayName("Login with correct credentials succeeds")
	void rq_login_3() {
		UserStore.add(UserConfig.newBuilder().setUsername("user123").setPassword("pass123").build());
		exe.rq_login(rq().setRqLogin(RQ_Login.newBuilder().setUsername("user123").setPassword("pass123")).build());

		assertTrue(((Message) channel.readOutbound()).getRsOutcome().getResult());
	}

	@Test
	@DisplayName("Login with an expired user fails")
	void rq_login_4() {
		UserStore.add(UserConfig.newBuilder().setUsername("user123").setPassword("pass123").setExpiration(123).build());
		exe.rq_login(rq().setRqLogin(RQ_Login.newBuilder().setUsername("user123").setPassword("pass123")).build());

		assertFalse(((Message) channel.readOutbound()).getRsOutcome().getResult());
	}
}
