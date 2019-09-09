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
package com.sandpolis.core.net.future;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.ConfigConstant.logging;
import com.sandpolis.core.instance.PoolConstant.net;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.handler.ExeletHandler;
import com.sandpolis.core.net.init.ChannelConstant;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;

class SockFutureTest {

	@BeforeAll
	static void configure() {
		Config.register(logging.net.traffic.raw, false);
		Config.register(logging.net.traffic.decoded, false);
		ThreadStore.init(config -> {
			config.ephemeral();
			config.defaults.put(net.exelet, new NioEventLoopGroup().next());
		});
	}

	@Test
	void testGetEmbedded() throws InterruptedException, ExecutionException {
		EmbeddedChannel server = new EmbeddedChannel();
		ChannelFuture serverFuture = server.bind(new InetSocketAddress(16101));

		EmbeddedChannel client = new EmbeddedChannel();
		ChannelFuture clientFuture = client.connect(new InetSocketAddress("127.0.0.1", 16101));

		testGet(serverFuture, clientFuture);
	}

	@Test
	void testGetNioTcp() throws InterruptedException, ExecutionException {
		ChannelFuture serverFuture = new ServerBootstrap().channel(NioServerSocketChannel.class)
				.group(new NioEventLoopGroup()).childHandler(new LoggingHandler()).bind(40255);

		ChannelFuture clientFuture = new Bootstrap().channel(NioSocketChannel.class).group(new NioEventLoopGroup())
				.handler(new LoggingHandler()).connect("127.0.0.1", 40255);

		testGet(serverFuture, clientFuture);
	}

	@Test
	void testGetNioUdp() throws InterruptedException, ExecutionException {
		ChannelFuture serverFuture = new Bootstrap().channel(NioDatagramChannel.class).group(new NioEventLoopGroup())
				.handler(new LoggingHandler()).bind(13418);

		ChannelFuture clientFuture = new Bootstrap().channel(NioDatagramChannel.class).group(new NioEventLoopGroup())
				.handler(new LoggingHandler()).connect("127.0.0.1", 13418);

		testGet(serverFuture, clientFuture);
	}

	private void testGet(ChannelFuture server, ChannelFuture client) throws InterruptedException, ExecutionException {

		// Set CVIDs manually
		server.channel().attr(ChannelConstant.CVID).set(123);
		client.channel().attr(ChannelConstant.CVID).set(321);

		// Set exelet handlers manually
		server.channel().attr(ChannelConstant.HANDLER_EXELET).set(new ExeletHandler(null));
		client.channel().attr(ChannelConstant.HANDLER_EXELET).set(new ExeletHandler(null));

		SockFuture sf = new SockFuture(client);
		sf.sync();

		assertTrue(sf.isDone());
		assertTrue(sf.isSuccess());
		assertNull(sf.cause());

		Sock sock = sf.get();
		sf.sync();

		assertTrue(client.isDone());
		assertTrue(client.isSuccess());
		assertNull(client.cause());

		assertNotNull(sock);
		assertEquals(client.channel(), sock.channel());
		assertEquals(sock, client.channel().attr(ChannelConstant.SOCK).get());

		client.channel().close();
		server.channel().close();
	}
}
