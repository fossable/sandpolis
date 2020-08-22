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
package com.sandpolis.core.net.cvid;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.Metatypes.InstanceFlavor;
import com.sandpolis.core.instance.Metatypes.InstanceType;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.channel.ChannelConstant;
import com.sandpolis.core.net.cvid.CvidRequestHandler;
import com.sandpolis.core.net.cvid.AbstractCvidHandler.CvidHandshakeCompletionEvent;
import com.sandpolis.core.net.msg.MsgCvid.RQ_Cvid;
import com.sandpolis.core.net.msg.MsgCvid.RS_Cvid;
import com.sandpolis.core.net.util.CvidUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;

class CvidRequestHandlerTest {

	private static final CvidRequestHandler HANDLER = new CvidRequestHandler();

	private EmbeddedChannel client;
	private CvidHandshakeCompletionEvent event;

	@BeforeEach
	void setup() {
		client = new EmbeddedChannel();
		event = null;

		client.pipeline().addFirst(new ChannelInboundHandlerAdapter() {
			@Override
			public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
				assertTrue(evt instanceof CvidHandshakeCompletionEvent);
				event = (CvidHandshakeCompletionEvent) evt;
			}
		});
		client.pipeline().addFirst(HANDLER);
	}

	@Test
	@DisplayName("Initiate a CVID handshake")
	void testInitiate() {
		HANDLER.handshake(client, InstanceType.CLIENT, InstanceFlavor.MEGA, "testuuid");

		MSG msg = client.readOutbound();
		RQ_Cvid rq = msg.getRqCvid();

		assertTrue(rq != null);
		assertEquals(InstanceType.CLIENT, rq.getInstance());
		assertEquals(InstanceFlavor.MEGA, rq.getInstanceFlavor());
		assertEquals("testuuid", rq.getUuid());
	}

	@Test
	@DisplayName("Receive an invalid response")
	void testReceiveIncorrect() {
		client.writeInbound(MSG.newBuilder().setRsCvid(RS_Cvid.newBuilder()).build());

		await().atMost(1000, TimeUnit.MILLISECONDS).until(() -> event != null);
		assertFalse(event.isSuccess());
		assertNull(client.pipeline().get(CvidRequestHandler.class), "Handler autoremove failed");
	}

	@Test
	@DisplayName("Receive a valid response")
	void testReceiveCorrect() {
		client.writeInbound(MSG.newBuilder().setRsCvid(RS_Cvid.newBuilder().setCvid(CvidUtil.cvid(InstanceType.CLIENT))
				.setServerCvid(123).setServerUuid("testuuid")).build());

		await().atMost(1000, TimeUnit.MILLISECONDS).until(() -> event != null);
		assertTrue(event.isSuccess());

		assertEquals(123, client.attr(ChannelConstant.CVID).get().intValue());
		assertEquals("testuuid", client.attr(ChannelConstant.UUID).get());
		assertNull(client.pipeline().get(CvidRequestHandler.class), "Handler autoremove failed");
	}

}
