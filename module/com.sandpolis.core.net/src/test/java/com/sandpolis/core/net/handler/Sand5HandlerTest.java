/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.PoolConstant.net;
import com.sandpolis.core.instance.store.thread.ThreadStore;
import com.sandpolis.core.util.CryptoUtil;
import com.sandpolis.core.util.CryptoUtil.SAND5.ReciprocalKeyPair;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;

class Sand5HandlerTest {

	private ReciprocalKeyPair serverKey;
	private ReciprocalKeyPair clientKey;

	private EmbeddedChannel serverChannel;
	private EmbeddedChannel clientChannel;

	@BeforeAll
	private static void init() {
		ThreadStore.register(new UnorderedThreadPoolEventExecutor(2), net.message.incoming);
	}

	@BeforeEach
	private void setup() {
		KeyPair k1 = CryptoUtil.SAND5.generate();
		KeyPair k2 = CryptoUtil.SAND5.generate();

		clientKey = new ReciprocalKeyPair(k1.getPrivate().getEncoded(), k2.getPublic().getEncoded());
		serverKey = new ReciprocalKeyPair(k2.getPrivate().getEncoded(), k1.getPublic().getEncoded());

		clientChannel = new EmbeddedChannel();
		serverChannel = new EmbeddedChannel();
	}

	@Test
	void testSuccess_1() throws Exception {
		Sand5Handler serverHandler = Sand5Handler.registerRequestHandler(serverChannel, serverKey);
		Sand5Handler clientHandler = Sand5Handler.registerResponseHandler(clientChannel, clientKey);

		assertFalse(serverHandler.challengeFuture().isDone());
		assertFalse(clientHandler.challengeFuture().isDone());

		// Manually transfer first phase
		clientChannel.writeInbound((ByteBuf) serverChannel.readOutbound());
		serverChannel.writeInbound((ByteBuf) clientChannel.readOutbound());

		assertFalse(serverHandler.challengeFuture().isDone());
		assertFalse(clientHandler.challengeFuture().isDone());

		// Manually transfer second phase
		serverChannel.writeInbound((ByteBuf) clientChannel.readOutbound());
		clientChannel.writeInbound((ByteBuf) serverChannel.readOutbound());

		assertTrue(serverHandler.challengeFuture().isDone());
		assertTrue(clientHandler.challengeFuture().isDone());

		assertTrue(serverHandler.challengeFuture().get());
		assertTrue(clientHandler.challengeFuture().get());

		assertNull(clientChannel.pipeline().get(Sand5Handler.class));
		assertNull(serverChannel.pipeline().get(Sand5Handler.class));
	}

	@Test
	@DisplayName("Try to use the wrong key")
	void testFailure_1() throws Exception {
		Sand5Handler serverHandler = Sand5Handler.registerRequestHandler(serverChannel, serverKey);
		Sand5Handler clientHandler = Sand5Handler.registerResponseHandler(clientChannel, serverKey);

		assertFalse(serverHandler.challengeFuture().isDone());
		assertFalse(clientHandler.challengeFuture().isDone());

		// Manually transfer first phase
		clientChannel.writeInbound((ByteBuf) serverChannel.readOutbound());
		serverChannel.writeInbound((ByteBuf) clientChannel.readOutbound());

		assertFalse(serverHandler.challengeFuture().isDone());
		assertFalse(clientHandler.challengeFuture().isDone());

		// Manually transfer second phase
		serverChannel.writeInbound((ByteBuf) clientChannel.readOutbound());
		clientChannel.writeInbound((ByteBuf) serverChannel.readOutbound());

		assertTrue(serverHandler.challengeFuture().isDone());
		assertTrue(clientHandler.challengeFuture().isDone());

		assertFalse(serverHandler.challengeFuture().get());
		assertFalse(clientHandler.challengeFuture().get());

		assertNull(clientChannel.pipeline().get(Sand5Handler.class));
		assertNull(serverChannel.pipeline().get(Sand5Handler.class));
	}

}
