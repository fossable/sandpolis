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

import static com.sandpolis.core.util.CryptoUtil.SHA256;
import static com.sandpolis.core.util.ProtoUtil.rq;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.storage.StoreProviderFactory;
import com.sandpolis.core.net.ExeletTest;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.proto.net.MCLogin.RQ_Login;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.util.CryptoUtil;
import com.sandpolis.core.util.RandUtil;
import com.sandpolis.server.store.user.User;
import com.sandpolis.server.store.user.UserStore;

public final class LoginExeTest extends ExeletTest {

	private LoginExe exe;

	@BeforeEach
	public void setup() {
		initChannel();
		exe = new LoginExe(new Sock(channel));

		UserStore.init(StoreProviderFactory.memoryList(User.class));
	}

	@Test
	public void testDeclaration() {
		testDeclaration(LoginExe.class);
	}

	@Test
	public void testLogin() {
		String user = RandUtil.nextAlphabetic(9);
		String pass = CryptoUtil.hash(SHA256, RandUtil.nextAlphabetic(16));

		exe.rq_login(rq().setRqLogin(RQ_Login.newBuilder().setUsername(user).setPassword(pass)).build());

		UserStore.add(user, pass, 0);

		exe.rq_login(rq().setRqLogin(RQ_Login.newBuilder().setUsername(user).setPassword(pass)).build());

		assertFalse(((Message) channel.readOutbound()).getRsOutcome().getResult());
		assertTrue(((Message) channel.readOutbound()).getRsOutcome().getResult());
	}

}
