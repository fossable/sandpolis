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
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.protobuf.Any;
import com.sandpolis.core.net.Message.MSG;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;

@Disabled
class ExeletHandlerTest {

	static boolean rq_test1_triggered;
	static boolean rq_test2_triggered;

	static ExeletMethod rq_test1_method;
	static ExeletMethod rq_test2_method;

	@BeforeAll
	static void configure() throws Exception {
		ThreadStore.init(config -> {
			config.defaults.put("net.message.incoming", new NioEventLoopGroup(1).next());
		});

		rq_test1_method = new ExeletMethod(Test1Exe.class.getMethod("rq_test1", MSG.class));
		rq_test2_method = new ExeletMethod(Test2Exe.class.getMethod("rq_test2", MSG.class));

	}

	@BeforeEach
	void setup() {
		rq_test1_triggered = false;
		rq_test2_triggered = false;
	}

	@Test
	void testUnauth() {
		var channel = new EmbeddedChannel();
		var connection = ConnectionStore.create(channel);
		var execute = new ExeletHandler(connection);
		channel.pipeline().addFirst(execute);

		execute.handlers.put("test1/com.sandpolis.core.net.MSG", rq_test1_method);
		execute.handlers.put("test2/com.sandpolis.core.net.MSG", rq_test2_method);

		channel.writeInbound(MSG.newBuilder().build());
		assertFalse(rq_test1_triggered);
		assertFalse(rq_test2_triggered);
		channel.writeInbound(MSG.newBuilder().setPayload(Any.pack(MSG.newBuilder().build(), "test1")).build());
		assertTrue(rq_test1_triggered);
		channel.writeInbound(MSG.newBuilder().setPayload(Any.pack(MSG.newBuilder().build(), "test2")).build());
		assertFalse(rq_test2_triggered);
	}

	@Test
	void testAuth() {
		var channel = new EmbeddedChannel();
		var connection = ConnectionStore.create(channel);
		var execute = new ExeletHandler(connection);
		channel.pipeline().addFirst(execute);
		connection.authenticate();

		execute.handlers.put("test1/com.sandpolis.core.net.MSG", rq_test1_method);
		execute.handlers.put("test2/com.sandpolis.core.net.MSG", rq_test2_method);

		channel.writeInbound(MSG.newBuilder().build());
		assertFalse(rq_test1_triggered);
		assertFalse(rq_test2_triggered);
		channel.writeInbound(MSG.newBuilder().setPayload(Any.pack(MSG.newBuilder().build(), "test1")).build());
		assertTrue(rq_test1_triggered);
		assertFalse(rq_test2_triggered);
		channel.writeInbound(MSG.newBuilder().setPayload(Any.pack(MSG.newBuilder().build(), "test2")).build());
		assertTrue(rq_test2_triggered);

	}

	public static class Test1Exe extends Exelet {

		@Handler(auth = false)
		public void rq_test1(MSG msg) {
			rq_test1_triggered = true;
		}
	}

	public static class Test2Exe extends Exelet {

		@Handler(auth = true)
		public void rq_test2(MSG msg) {
			rq_test2_triggered = true;
		}
	}
}
