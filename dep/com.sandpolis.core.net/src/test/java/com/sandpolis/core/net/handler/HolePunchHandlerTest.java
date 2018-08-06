/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.core.net.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Test the {@link HolePunchHandler} by simulating random message sequences.
 * This class is designed to emulate the unreliability of UDP.
 */
class HolePunchHandlerTest {

	/**
	 * Simulates the first application handler in the pipeline.
	 */
	ChannelInboundHandlerAdapter application = new ChannelInboundHandlerAdapter() {

		private final ByteBuf message = Unpooled.wrappedBuffer("Handshake".getBytes());

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {

			// Send a fake handshake message
			ctx.writeAndFlush(message.retain());
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			assertEquals(message, msg);
		}

		@Override
		public boolean isSharable() {
			return true;
		}
	};

	/**
	 * Simulates an unreliable connection.
	 */
	private class UnreliableHandler extends ChannelInboundHandlerAdapter {

		private Iterator<Boolean> seed;

		public UnreliableHandler(boolean[] seed) {
			// Dear Java, please allow me to stream from a boolean array someday.
			// this.seed = Arrays.stream(seed).iterator();

			List<Boolean> list = new ArrayList<>();
			for (boolean b : seed)
				list.add(b);
			this.seed = list.iterator();
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (ctx.channel().pipeline().get(HolePunchHandler.class) == null) {
				// The handshake is complete so forward every message
				ctx.fireChannelRead(msg);
				return;
			}

			if (seed.hasNext() && seed.next()) {
				ctx.fireChannelRead(msg);
				return;
			}
		}
	};

	public class TestInitializer extends ChannelInitializer<Channel> {

		private HolePunchHandler handler;
		private boolean[] seed;

		public TestInitializer(HolePunchHandler handler, boolean[] seed) {
			this.handler = handler;
			this.seed = seed;
		}

		@Override
		protected void initChannel(Channel ch) throws Exception {
			ch.pipeline().addLast(new UnreliableHandler(seed));
			ch.pipeline().addLast(handler);
			ch.pipeline().addLast(application);
		}
	}

	@Test
	void testNoLoss() throws Exception {
		testPartialLoss(new boolean[] { true, true, true }, new boolean[] { true, true, true }, true);
	}

	@Test
	void testTotalLoss() throws Exception {
		testPartialLoss(new boolean[] { false, false, false }, new boolean[] { false, false, false }, false);
	}

	@Test
	void testPartialLoss() throws Exception {
//		testPartialLoss(new boolean[] { true, true, true }, new boolean[] { false, true, true }, true);

		testPartialLoss(new boolean[] { true, false, true }, new boolean[] { false, false, false }, false);
		testPartialLoss(new boolean[] { false, false, false }, new boolean[] { true, false, true }, false);
		testPartialLoss(new boolean[] { true, true, true }, new boolean[] { false, false, false }, false);
		testPartialLoss(new boolean[] { false, false, false }, new boolean[] { true, true, true }, false);
		testPartialLoss(new boolean[] { true, false, false }, new boolean[] { false, false, true }, false);
		testPartialLoss(new boolean[] { false, false, true }, new boolean[] { true, false, false }, false);
	}

	private void testPartialLoss(boolean[] seed1, boolean[] seed2, boolean result) throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();

		try {
			HolePunchHandler h1 = new HolePunchHandler();
			HolePunchHandler h2 = new HolePunchHandler();

			new ServerBootstrap().channel(NioServerSocketChannel.class).group(group)
					.childHandler(new TestInitializer(h1, seed1)).bind(8000).sync();

			ChannelFuture client = new Bootstrap().channel(NioSocketChannel.class).group(group)
					.handler(new TestInitializer(h2, seed2)).connect("127.0.0.1", 8000).sync();

			if (result) {
				assertSuccess(h1);
				assertSuccess(h2, client.channel());
			} else {
				assertFailure(h1);
				assertFailure(h2, client.channel());
			}
		} finally {
			group.shutdownGracefully();
		}
	}

	/**
	 * Assert that the given handler and channel have the characteristics of a
	 * successful hole-punch.
	 */
	private void assertSuccess(HolePunchHandler h, Channel c) throws Exception {
		assertSuccess(h);

		assertEquals(c, h.handshakeFuture().get());

		// Check pipeline
		assertNull(c.pipeline().get(HolePunchHandler.class));
	}

	/**
	 * Assert that the given handler has the characteristics of a successful
	 * hole-punch.
	 */
	private void assertSuccess(HolePunchHandler h) throws Exception {

		// Wait for handshake
		assertTrue(h.handshakeFuture().await().isSuccess());
		assertNotNull(h.handshakeFuture().get());
	}

	/**
	 * Assert that the given handler and channel do not have the characteristics of
	 * a successful hole-punch.
	 */
	private void assertFailure(HolePunchHandler h, Channel c) throws Exception {
		assertFailure(h);

		// Check pipeline
		assertNotNull(c.pipeline().get(HolePunchHandler.class));
	}

	/**
	 * Assert that the given handler does not have the characteristics of a
	 * successful hole-punch.
	 */
	private void assertFailure(HolePunchHandler h) throws Exception {

		// Wait for handshake
		assertTrue(h.handshakeFuture().await().isSuccess());
		assertNull(h.handshakeFuture().get());
	}
}
