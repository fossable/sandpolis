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
package com.sandpolis.core.net.connection;

import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.net.UnitSock;
import com.sandpolis.core.net.channel.ChannelConstant;
import com.sandpolis.core.net.connection.Connection;
import com.sandpolis.core.net.connection.ConnectionFuture;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Promise;

class ConnectionFutureTest {

	@BeforeAll
	static void configure() {
		ThreadStore.init(config -> {
			config.ephemeral();
			config.defaults.put("net.exelet", new NioEventLoopGroup().next());
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

		// Create Sock
		var s = new UnitSock(client.channel());
		client.channel().attr(ChannelConstant.SOCK).set(s);
		((Promise<Void>) s.getHandshakeFuture()).setSuccess(null);

		ConnectionFuture sf = new ConnectionFuture(client);
		sf.sync();

		assertTrue(sf.isDone());
		assertTrue(sf.isSuccess());
		assertNull(sf.cause());

		Connection sock = sf.get();
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
