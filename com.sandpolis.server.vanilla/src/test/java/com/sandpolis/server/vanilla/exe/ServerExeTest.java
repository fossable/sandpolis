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
package com.sandpolis.server.vanilla.exe;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.net.MsgPing.RQ_Ping;
import com.sandpolis.core.net.MsgPing.RS_Ping;
import com.sandpolis.core.net.command.ExeletTest;

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
