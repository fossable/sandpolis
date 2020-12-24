//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.server.listener;

import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.server.listener.ListenerStore.ListenerStore;
import static com.sandpolis.core.server.user.UserStore.UserStore;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.clientserver.msg.MsgListener.RQ_ListenerOperation;
import com.sandpolis.core.foundation.Result.Outcome;
import com.sandpolis.core.instance.Listener.ListenerConfig;
import com.sandpolis.core.instance.User.UserConfig;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.exelet.ExeletContext;

import io.netty.channel.embedded.EmbeddedChannel;

@Disabled
class ListenerExeTest {

	protected ExeletContext context;

	@BeforeEach
	void setup() {
		UserStore.init(config -> {
		});
		UserStore.create(UserConfig.newBuilder().setUsername("junit").setPassword("12345678").build());
		ListenerStore.init(config -> {
		});
		ListenerStore.create(ListenerConfig.newBuilder().setOwner("junit").setPort(5000).setAddress("0.0.0.0").build());
		ThreadStore.init(config -> {
			config.defaults.put("store.event_bus", Executors.newSingleThreadExecutor());
		});

		context = new ExeletContext(ConnectionStore.create(new EmbeddedChannel()), MSG.newBuilder().build());
	}

	@Test
	void testDeclaration() {
	}

	@Test
	@DisplayName("Add a listener with a valid configuration")
	void rq_add_listener_1() {
		var rq = RQ_ListenerOperation.newBuilder()
				.addListenerConfig(
						ListenerConfig.newBuilder().setId(2).setOwner("junit").setPort(5000).setAddress("0.0.0.0"))
				.build();
		var rs = ListenerExe.rq_listener_operation(context, rq);

		assertTrue(((Outcome) rs).getResult());
	}

}
