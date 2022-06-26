//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.banner;

import static org.s7s.core.instance.thread.ThreadStore.ThreadStore;
import static org.s7s.core.instance.connection.ConnectionStore.ConnectionStore;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.s7s.core.instance.msg.MsgPing.RQ_Ping;
import org.s7s.core.instance.msg.MsgPing.RS_Ping;
import org.s7s.core.protocol.Message.MSG;
import org.s7s.core.instance.exelet.ExeletContext;

import io.netty.channel.embedded.EmbeddedChannel;

@Disabled
class BannerExeTest {

	protected ExeletContext context;

	@Test
	@DisplayName("Check that pings get a response")
	void rq_ping_1() {
		var rq = RQ_Ping.newBuilder().build();
		var rs = BannerExe.rq_ping(rq);

		assertEquals(RS_Ping.newBuilder().build(), ((RS_Ping.Builder) rs).build());
	}

	@BeforeEach
	void setup() {
		ThreadStore.init(config -> {
			config.defaults.put("store.event_bus", Executors.newSingleThreadExecutor());
		});

		context = new ExeletContext(ConnectionStore.create(new EmbeddedChannel()), MSG.newBuilder().build());
	}

	@Test
	void testDeclaration() {
	}
}
