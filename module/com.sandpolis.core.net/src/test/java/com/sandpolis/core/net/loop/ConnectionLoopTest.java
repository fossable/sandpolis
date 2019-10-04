/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.core.net.loop;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
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
			config.defaults.put("temploop", GlobalEventExecutor.INSTANCE);
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
