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
package com.sandpolis.core.net.exelet;

import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.net.UnitSock;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.MsgCvid.RQ_Cvid;
import com.sandpolis.core.net.MsgLogin.RQ_Login;
import com.sandpolis.core.net.exelet.Exelet;
import com.sandpolis.core.net.exelet.ExeletHandler;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;

@Disabled
class ExeletHandlerTest {

	static boolean rq_login_triggered;
	static boolean rq_cvid_triggered;

	@BeforeAll
	static void configure() {
		ThreadStore.init(config -> {
			config.ephemeral();
			config.defaults.put("net.message.incoming", new NioEventLoopGroup(1).next());
		});
	}

	@BeforeEach
	void setup() {
		rq_login_triggered = false;
		rq_cvid_triggered = false;
	}

	@Test
	void testUnauth() {
		EmbeddedChannel channel = new EmbeddedChannel();
		ExeletHandler execute = new ExeletHandler(new UnitSock(channel));
		channel.pipeline().addFirst(execute);
		execute.register(TestExe.class, Test2Exe.class);

		channel.writeInbound(MSG.newBuilder().build());
		assertFalse(rq_login_triggered);
		assertFalse(rq_cvid_triggered);
		channel.writeInbound(MSG.newBuilder().setRqLogin(RQ_Login.newBuilder()).build());
		assertTrue(rq_login_triggered);
		channel.writeInbound(MSG.newBuilder().setRqCvid(RQ_Cvid.newBuilder()).build());
		assertFalse(rq_cvid_triggered);
	}

	@Test
	void testAuth() {
		EmbeddedChannel channel = new EmbeddedChannel();
		ExeletHandler execute = new ExeletHandler(new UnitSock(channel));
		channel.pipeline().addFirst(execute);
		execute.register(TestExe.class, Test2Exe.class);
		execute.authenticate();

		channel.writeInbound(MSG.newBuilder().build());
		assertFalse(rq_login_triggered);
		assertFalse(rq_cvid_triggered);
		channel.writeInbound(MSG.newBuilder().setRqLogin(RQ_Login.newBuilder()).build());
		assertTrue(rq_login_triggered);
		assertFalse(rq_cvid_triggered);
		channel.writeInbound(MSG.newBuilder().setRqCvid(RQ_Cvid.newBuilder()).build());
		assertTrue(rq_cvid_triggered);

	}

	// Sandbox Exelets
	public static class TestExe extends Exelet {

		@Unauth
		@Handler(tag = MSG.RQ_LOGIN_FIELD_NUMBER)
		public void rq_login(MSG msg) {
			rq_login_triggered = true;
		}

	}

	public static class Test2Exe extends Exelet {

		@Auth
		@Handler(tag = MSG.RQ_CVID_FIELD_NUMBER)
		public void rq_cvid(MSG msg) {
			rq_cvid_triggered = true;
		}

	}

}
