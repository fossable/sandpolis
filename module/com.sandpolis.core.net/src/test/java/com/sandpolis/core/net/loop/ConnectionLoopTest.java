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
package com.sandpolis.core.net.loop;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

class ConnectionLoopTest {

	@BeforeAll
	private static void setup() {
		ThreadStore.init(config -> {
			config.ephemeral();
			config.defaults.put("net.connection.outgoing", GlobalEventExecutor.INSTANCE);
			config.defaults.put("net.connection.loop", GlobalEventExecutor.INSTANCE);
		});
	}

	@Test
	@DisplayName("Attempt a connection on a closed port")
	void connect_1() throws InterruptedException {
		ConnectionLoop loop = new ConnectionLoop("127.0.0.1", 38903, 500, new Bootstrap()
				.channel(NioSocketChannel.class).group(new NioEventLoopGroup()).handler(new LoggingHandler()));

		assertFalse(loop.future().isDone());
		assertFalse(loop.future().isSuccess());

		loop.start().await(1000, TimeUnit.MILLISECONDS);

		assertTrue(loop.future().isDone());
		assertTrue(loop.future().isSuccess());
		assertNull(loop.future().getNow());
	}

	@Test
	@Disabled
	@DisplayName("Make a successful connection to a local socket")
	void connect_2() throws InterruptedException, IOException {

		new ServerBootstrap().group(new NioEventLoopGroup()).channel(NioServerSocketChannel.class)
				.childHandler(new LoggingHandler()).bind(InetAddress.getLoopbackAddress(), 23374).sync();

		ConnectionLoop loop = new ConnectionLoop("127.0.0.1", 23374, 500, new Bootstrap()
				.channel(NioSocketChannel.class).group(new NioEventLoopGroup()).handler(new LoggingHandler()));

		assertFalse(loop.future().isDone());
		assertFalse(loop.future().isSuccess());

		loop.start().await(1000, TimeUnit.MILLISECONDS);

		assertTrue(loop.future().isDone());
		assertTrue(loop.future().isSuccess());
	}
}
