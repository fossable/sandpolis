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
package com.sandpolis.core.net.future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.init.ChannelConstant;
import com.sandpolis.core.net.init.PeerPipelineInit;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;

public class SockFutureTest {

	private static PeerPipelineInit clientPipeline = new PeerPipelineInit(null);
	private static PeerPipelineInit serverPipeline = new PeerPipelineInit(null);

	@Test
	public void testGetEmbedded() throws InterruptedException, ExecutionException {
		EmbeddedChannel server = new EmbeddedChannel(serverPipeline);
		ChannelFuture serverFuture = server.bind(new InetSocketAddress(9000));

		EmbeddedChannel client = new EmbeddedChannel(clientPipeline);
		ChannelFuture clientFuture = client.connect(new InetSocketAddress("127.0.0.1", 9000));

		testGet(serverFuture, clientFuture);
	}

	@Test
	public void testGetNioTcp() throws InterruptedException, ExecutionException {
		ChannelFuture serverFuture = new ServerBootstrap().channel(NioServerSocketChannel.class)
				.group(new NioEventLoopGroup()).childHandler(serverPipeline).bind(9000);

		ChannelFuture clientFuture = new Bootstrap().channel(NioSocketChannel.class).group(new NioEventLoopGroup())
				.handler(clientPipeline).connect("127.0.0.1", 9000);

		testGet(serverFuture, clientFuture);
	}

	@Test
	public void testGetNioUdp() throws InterruptedException, ExecutionException {
		ChannelFuture serverFuture = new Bootstrap().channel(NioDatagramChannel.class).group(new NioEventLoopGroup())
				.handler(serverPipeline).bind(9000);

		ChannelFuture clientFuture = new Bootstrap().channel(NioDatagramChannel.class).group(new NioEventLoopGroup())
				.handler(clientPipeline).connect("127.0.0.1", 9000);

		testGet(serverFuture, clientFuture);
	}

	private void testGet(ChannelFuture server, ChannelFuture client) throws InterruptedException, ExecutionException {

		// Complete the CVID handshake manually
		server.channel().attr(ChannelConstant.FUTURE_CVID)
				.set(new DefaultPromise<Integer>(server.channel().eventLoop()).setSuccess(1));
		client.channel().attr(ChannelConstant.FUTURE_CVID)
				.set(new DefaultPromise<Integer>(client.channel().eventLoop()).setSuccess(1));

		SockFuture sf = new SockFuture(client);

		client.sync();

		Sock sock = sf.get();
		assertTrue(sf.isDone());
		assertTrue(sf.isSuccess());
		assertNull(sf.cause());

		assertNotNull(sock);
		assertEquals(client.channel(), sock.channel());
		assertEquals(sock, client.channel().attr(ChannelConstant.SOCK).get());

		server.channel().close();
		client.channel().close();
	}

}
