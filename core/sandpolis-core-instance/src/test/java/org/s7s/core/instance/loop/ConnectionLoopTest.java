//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.loop;

import static org.s7s.core.instance.thread.ThreadStore.ThreadStore;
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

import org.s7s.core.instance.connection.ConnectionLoop;

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
			config.defaults.put("net.connection.outgoing", GlobalEventExecutor.INSTANCE);
			config.defaults.put("net.connection.loop", GlobalEventExecutor.INSTANCE);
		});
	}

	@Test
	@DisplayName("Attempt a connection on a closed port")
	void testConnectClosedPort() throws InterruptedException {
		ConnectionLoop loop = new ConnectionLoop(config -> {
			config.timeout = 100;
			config.iterationLimit = 1;
			config.address("example.com", 38903);
			config.bootstrap.channel(NioSocketChannel.class).group(new NioEventLoopGroup())
					.handler(new LoggingHandler());
		});

		assertFalse(loop.future().isDone());
		assertFalse(loop.future().isSuccess());

		loop.start().future().await(1000, TimeUnit.MILLISECONDS);

		assertTrue(loop.future().isDone());
		assertFalse(loop.future().isSuccess());
		assertNull(loop.future().getNow());
	}

	@Test
	@Disabled
	@DisplayName("Make a successful connection to a local socket")
	void testConnectionSuccess() throws InterruptedException, IOException {

		new ServerBootstrap().group(new NioEventLoopGroup()).channel(NioServerSocketChannel.class)
				.childHandler(new LoggingHandler()).bind(InetAddress.getLoopbackAddress(), 23374).sync();

		ConnectionLoop loop = new ConnectionLoop(config -> {
			config.iterationLimit = 1;
			config.address("127.0.0.1", 23374);
			config.bootstrap.channel(NioSocketChannel.class).group(new NioEventLoopGroup())
					.handler(new LoggingHandler());
		});

		assertFalse(loop.future().isDone());
		assertFalse(loop.future().isSuccess());

		loop.start().future().await(1000, TimeUnit.MILLISECONDS);

		assertTrue(loop.future().isDone());
		assertTrue(loop.future().isSuccess());
	}
}
