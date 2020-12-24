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
package com.sandpolis.core.server.auth;

import static com.sandpolis.core.instance.profile.ProfileStore.ProfileStore;
import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.server.user.UserStore.UserStore;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.clientserver.msg.MsgLogin.RQ_Login;
import com.sandpolis.core.foundation.Result.Outcome;
import com.sandpolis.core.instance.User.UserConfig;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.exelet.ExeletContext;

import io.netty.channel.embedded.EmbeddedChannel;

@Disabled
class LoginExeTest {

	protected ExeletContext context;

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
		UserStore.create(UserConfig.newBuilder().setUsername("user123").setPassword("pass123").build());
		var rq = RQ_Login.newBuilder().setUsername("user123").setPassword("pass1234").build();
		var rs = LoginExe.rq_login(context, rq);

		assertFalse(((Outcome) rs).getResult());
	}

	@Test
	@DisplayName("Login with correct credentials succeeds")
	void rq_login_3() {
		UserStore.create(UserConfig.newBuilder().setUsername("user123").setPassword("pass123").build());
		var rq = RQ_Login.newBuilder().setUsername("user123").setPassword("pass123").build();
		var rs = LoginExe.rq_login(context, rq);

		assertTrue(((Outcome) rs).getResult());
	}

	@Test
	@DisplayName("Login with an expired user fails")
	void rq_login_4() {
		UserStore.create(
				UserConfig.newBuilder().setUsername("user123").setPassword("pass123").setExpiration(123).build());
		var rq = RQ_Login.newBuilder().setUsername("user123").setPassword("pass123").build();
		var rs = LoginExe.rq_login(context, rq);

		assertFalse(((Outcome) rs).getResult());
	}

	@BeforeEach
	void setup() {
		UserStore.init(config -> {
		});
		ProfileStore.init(config -> {
		});
		ThreadStore.init(config -> {
			config.defaults.put("store.event_bus", Executors.newSingleThreadExecutor());
		});

		var channel = new EmbeddedChannel();

		context = new ExeletContext(ConnectionStore.create(channel), MSG.newBuilder().build());
	}

	@Test
	void testDeclaration() {
	}
}
