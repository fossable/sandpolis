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
package com.sandpolis.core.net.handler.cvid;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.Metatypes.InstanceType;
import com.sandpolis.core.net.ChannelConstant;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.handler.cvid.AbstractCvidHandler.CvidHandshakeCompletionEvent;
import com.sandpolis.core.net.msg.MsgCvid.RQ_Cvid;
import com.sandpolis.core.net.msg.MsgCvid.RS_Cvid;
import com.sandpolis.core.net.util.CvidUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;

class CvidResponseHandlerTest {

	private static final CvidResponseHandler HANDLER = new CvidResponseHandler();

	private EmbeddedChannel server;
	private CvidHandshakeCompletionEvent event;

	@BeforeEach
	void setup() {
		server = new EmbeddedChannel();
		event = null;

		server.pipeline().addFirst(new ChannelInboundHandlerAdapter() {
			@Override
			public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
				assertTrue(evt instanceof CvidHandshakeCompletionEvent);
				event = (CvidHandshakeCompletionEvent) evt;
			}
		});
		server.pipeline().addFirst(HANDLER);
	}

	@Test
	@DisplayName("Receive an invalid response")
	void testReceiveIncorrect() {
		server.writeInbound(MSG.newBuilder()
				.setRqCvid(RQ_Cvid.newBuilder().setInstance(InstanceType.SERVER).setUuid("testuuid2")).build());

		await().atMost(1000, TimeUnit.MILLISECONDS).until(() -> event != null);
		assertFalse(event.isSuccess());
		assertNull(server.pipeline().get(CvidResponseHandler.class), "Handler autoremove failed");
	}

	@Test
	@DisplayName("Receive a valid response")
	void testReceiveCorrect() {
		server.writeInbound(MSG.newBuilder()
				.setRqCvid(RQ_Cvid.newBuilder().setInstance(InstanceType.CLIENT).setUuid("testuuid2")).build());

		await().atMost(1000, TimeUnit.MILLISECONDS).until(() -> event != null);
		assertTrue(event.isSuccess());

		assertEquals(InstanceType.CLIENT, CvidUtil.extractInstance(server.attr(ChannelConstant.CVID).get()));
		assertEquals("testuuid2", server.attr(ChannelConstant.UUID).get());
		assertNull(server.pipeline().get(CvidResponseHandler.class), "Handler autoremove failed");

		MSG msg = server.readOutbound();
		RS_Cvid rs = msg.getRsCvid();

		assertEquals(InstanceType.CLIENT, CvidUtil.extractInstance(rs.getCvid()));
		assertFalse(rs.getServerUuid().isEmpty());

	}

}
