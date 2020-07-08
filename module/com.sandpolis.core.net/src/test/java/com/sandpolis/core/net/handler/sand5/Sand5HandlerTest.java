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
package com.sandpolis.core.net.handler.sand5;

import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.foundation.util.CryptoUtil;

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
		ThreadStore.init(config -> {
			config.ephemeral();
			config.defaults.put("net.message.incoming", new UnorderedThreadPoolEventExecutor(2));
		});
	}

	@BeforeEach
	private void setup() {
		KeyPair k1 = CryptoUtil.SAND5.generate();
		KeyPair k2 = CryptoUtil.SAND5.generate();

		clientKey = new ReciprocalKeyPair(k1.getPrivate().getEncoded(), k2.getPublic().getEncoded());
		serverKey = new ReciprocalKeyPair(k2.getPrivate().getEncoded(), k1.getPublic().getEncoded());

		clientChannel = null;
		serverChannel = null;
	}

	@Test
	void testSuccess_1() throws Exception {
		Sand5Handler serverHandler = Sand5Handler.newRequestHandler(serverKey);
		serverChannel = new EmbeddedChannel(serverHandler);
		Sand5Handler clientHandler = Sand5Handler.newResponseHandler(clientKey);
		clientChannel = new EmbeddedChannel(clientHandler);

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
		Sand5Handler serverHandler = Sand5Handler.newRequestHandler(serverKey);
		serverChannel = new EmbeddedChannel(serverHandler);
		Sand5Handler clientHandler = Sand5Handler.newResponseHandler(serverKey);
		clientChannel = new EmbeddedChannel(clientHandler);

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
