//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.channel.peer;

import static org.s7s.core.instance.thread.ThreadStore.ThreadStore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.s7s.core.foundation.S7SRandom;
import org.s7s.core.protocol.Message.MSG;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

@Disabled
class PeerChannelInitializerTest {

	private final PeerChannelInitializer init = new PeerChannelInitializer(config -> {
	});

	@BeforeAll
	static void configure() {

		ThreadStore.init(config -> {
			config.defaults.put("net.exelet", new NioEventLoopGroup().next());
		});
	}

	@Test
	void testNioUdp() throws InterruptedException {
		Channel peer1 = new Bootstrap().group(new NioEventLoopGroup()).channel(NioDatagramChannel.class).handler(init)
				.bind(30492).sync().channel();

		Channel peer2 = new Bootstrap().group(new NioEventLoopGroup()).channel(NioDatagramChannel.class).handler(init)
				.connect("127.0.0.1", 30482).sync().channel();

		exchange(peer1, peer2);
		exchange(peer2, peer1);
	}

	@Test
	void testNioTcp() throws InterruptedException {
		Channel server = new ServerBootstrap().group(new NioEventLoopGroup()).channel(NioServerSocketChannel.class)
				.childHandler(init).bind(28551).sync().channel();

		Channel client = new Bootstrap().group(new NioEventLoopGroup()).channel(NioSocketChannel.class).handler(init)
				.connect("127.0.0.1", 28551).sync().channel();

		exchange(server, client);
		exchange(client, server);
	}

	/**
	 * Exchange a random message between two peers and check for validity.
	 */
	private void exchange(Channel peer1, Channel peer2) {
		MSG rand1 = MSG.newBuilder().setId(S7SRandom.nextNonzeroInt()).build();
		MSG rand2 = MSG.newBuilder().setId(S7SRandom.nextNonzeroInt()).build();

		peer1.pipeline().addLast(new ChannelInboundHandlerAdapter() {
			@Override
			public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
				assertEquals(rand2, msg);
				ctx.pipeline().remove(this);
			}
		});
		peer2.pipeline().addLast(new ChannelInboundHandlerAdapter() {
			@Override
			public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
				assertEquals(rand1, msg);
				ctx.pipeline().remove(this);
			}
		});

		peer1.writeAndFlush(rand1);
		peer2.writeAndFlush(rand2);

		assertNull(peer1.pipeline().get("peer1"));
		assertNull(peer2.pipeline().get("peer2"));

	}

}
