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

import java.util.Arrays;
import java.util.Iterator;

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
 * Test the {@link HolePunchHandler} by simulating every possible message loss
 * pattern during the hole-punching phase.
 */
class HolePunchHandlerTest {

	/**
	 * Simulates the first application handler in the pipeline.
	 */
	ChannelInboundHandlerAdapter application = new ChannelInboundHandlerAdapter() {

		/**
		 * An example application message.
		 */
		private final ByteBuf message = Unpooled.wrappedBuffer("Handshake".getBytes());

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			// Send a fake handshake message
			ctx.writeAndFlush(message.retain());
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			// Ensure the message gets through
			assertEquals(message, msg);
		}

		@Override
		public boolean isSharable() {
			return true;
		}
	};

	/**
	 * A handler that simulates an unreliable medium by dropping messages according
	 * to a seed.
	 */
	private class UnreliableHandler extends ChannelInboundHandlerAdapter {

		/**
		 * Indicates which messages should be dropped.
		 */
		private Iterator<Boolean> seed;

		public UnreliableHandler(Boolean... seed) {
			this.seed = Arrays.stream(seed).iterator();
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (ctx.channel().pipeline().get(HolePunchHandler.class) == null) {
				// The handshake is complete so forward every message now
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
		private Boolean[] seed;

		public TestInitializer(HolePunchHandler handler, Boolean[] seed) {
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

	// @Test
	void lossPattern_0() throws Exception {
		testLossPattern(new Boolean[] { true, true, true }, new Boolean[] { true, true, true }, true);
	}

	// @Test
	void lossPattern_1() throws Exception {
		testLossPattern(new Boolean[] { true, true, true }, new Boolean[] { false, true, true }, true);
	}

	@Test
	void lossPattern_2() throws Exception {
		testLossPattern(new Boolean[] { true, true, true }, new Boolean[] { false, false, false }, false);
	}

	@Test
	void lossPattern_3() throws Exception {
		testLossPattern(new Boolean[] { true, false, true }, new Boolean[] { false, false, false }, false);
	}

	@Test
	void lossPattern_4() throws Exception {
		testLossPattern(new Boolean[] { true, false, false }, new Boolean[] { false, false, true }, false);
	}

	@Test
	void lossPattern_5() throws Exception {
		testLossPattern(new Boolean[] { false, false, true }, new Boolean[] { true, false, false }, false);
	}

	@Test
	void lossPattern_6() throws Exception {
		testLossPattern(new Boolean[] { false, false, false }, new Boolean[] { true, true, true }, false);
	}

	@Test
	void lossPattern_7() throws Exception {
		testLossPattern(new Boolean[] { false, false, false }, new Boolean[] { true, false, true }, false);
	}

	@Test
	void lossPattern_8() throws Exception {
		testLossPattern(new Boolean[] { false, false, false }, new Boolean[] { false, false, false }, false);
	}

	/**
	 * Build channels using {@link TestInitializer} according to the given seeds and
	 * run the hole-puncher.
	 * 
	 * @param seed1  The server seed
	 * @param seed2  The client seed
	 * @param result The expected result of the hole-punch according to the seeds
	 * @throws Exception
	 */
	private void testLossPattern(Boolean[] seed1, Boolean[] seed2, boolean result) throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();

		try {
			HolePunchHandler h1 = new HolePunchHandler();
			HolePunchHandler h2 = new HolePunchHandler();

			new ServerBootstrap().channel(NioServerSocketChannel.class).group(group)
					.childHandler(new TestInitializer(h1, seed1)).bind(17834).sync();

			ChannelFuture client = new Bootstrap().channel(NioSocketChannel.class).group(group)
					.handler(new TestInitializer(h2, seed2)).connect("127.0.0.1", 17834).sync();

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
		assertTrue(h.handshakeFuture().sync().isSuccess());
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
		assertTrue(h.handshakeFuture().sync().isSuccess());
		assertNull(h.handshakeFuture().get());
	}
}
