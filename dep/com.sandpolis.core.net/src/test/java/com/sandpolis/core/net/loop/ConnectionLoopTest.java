/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
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
package com.sandpolis.core.net.loop;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;

class ConnectionLoopTest {

	private EventLoopGroup group;

	@BeforeEach
	private void setup() {
		group = new NioEventLoopGroup();
	}

	@AfterEach
	private void cleanup() {
		group.shutdownGracefully();
	}

	@Test
	void testConnectionFailure() throws InterruptedException {
		ConnectionLoop loop = new ConnectionLoop("127.0.0.1", 6001, 100,
				new Bootstrap().channel(NioSocketChannel.class).group(group).handler(new LoggingHandler()));

		long t1 = System.currentTimeMillis();
		loop.start();
		loop.await();
		assertTrue(System.currentTimeMillis() - t1 >= 100, "Timeout not reached");

		assertNull(loop.getResult());
	}

	@Test
	void testSingletonSuccess() throws InterruptedException, IOException {

		new ServerBootstrap().group(group).channel(NioServerSocketChannel.class).childHandler(new LoggingHandler())
				.bind(6001);

		ConnectionLoop loop = new ConnectionLoop("127.0.0.1", 6001, 100,
				new Bootstrap().channel(NioSocketChannel.class).group(group).handler(new LoggingHandler()));

		long t1 = System.currentTimeMillis();
		loop.start();
		loop.await();
		assertTrue(System.currentTimeMillis() - t1 < 100, "Timeout exceeded");

		assertNotNull(loop.getResult());
	}
}
