//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.session;

import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.protobuf.InvalidProtocolBufferException;
import org.s7s.core.protocol.Session.RQ_Session;
import org.s7s.core.protocol.Session.RS_Session;
import org.s7s.core.foundation.Instance.InstanceFlavor;
import org.s7s.core.foundation.Instance.InstanceType;
import org.s7s.core.instance.channel.ChannelConstant;
import org.s7s.core.instance.session.AbstractSessionHandler.SessionHandshakeCompletionEvent;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ConnectionOid;
import org.s7s.core.instance.util.S7SMsg;
import org.s7s.core.instance.util.S7SSessionID;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;

@Disabled
class SessionRequestHandlerTest {

	private static final SessionRequestHandler HANDLER = new SessionRequestHandler();

	private EmbeddedChannel client;
	private SessionHandshakeCompletionEvent event;

	@BeforeEach
	void setup() {
		client = new EmbeddedChannel();
		event = null;

		client.pipeline().addFirst(new ChannelInboundHandlerAdapter() {
			@Override
			public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
				assertTrue(evt instanceof SessionHandshakeCompletionEvent);
				event = (SessionHandshakeCompletionEvent) evt;
			}

			@Override
			public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
				fail(cause);
			}
		});
		client.pipeline().addFirst(HANDLER);
	}

	@Test
	@DisplayName("Initiate a SID handshake")
	void testInitiate() throws InvalidProtocolBufferException {
		var testUuid = randomUUID().toString();
		HANDLER.handshake(client, InstanceType.CLIENT, InstanceFlavor.GENERIC, testUuid);

		RQ_Session rq = S7SMsg.of(client.readOutbound()).unpack(RQ_Session.class);

		assertTrue(rq != null);
		assertEquals(InstanceType.CLIENT, rq.getInstanceType());
		assertEquals(InstanceFlavor.GENERIC, rq.getInstanceFlavor());
		assertEquals(testUuid, rq.getInstanceUuid());
	}

	@Test
	@DisplayName("Receive an invalid response")
	void testReceiveIncorrect() {
		client.writeInbound(S7SMsg.rs(123).pack(RS_Session.newBuilder()).build());

		await().atMost(1000, TimeUnit.MILLISECONDS).until(() -> event != null);
		assertFalse(event.success);
		assertNull(client.pipeline().get(SessionRequestHandler.class), "Handler autoremove failed");
	}

	@Test
	@DisplayName("Receive a valid response")
	void testReceiveCorrect() {
		var testUuid = randomUUID().toString();
		var testCvid = 123456;
		client.writeInbound(S7SMsg.rs(123)
				.pack(RS_Session.newBuilder()
						.setInstanceSid(S7SSessionID.of(InstanceType.CLIENT, InstanceFlavor.GENERIC).sid())
						.setServerSid(testCvid).setServerUuid(testUuid))
				.build());

		await().atMost(1000, TimeUnit.MILLISECONDS).until(() -> event != null);
		assertTrue(event.success);

		assertEquals(testCvid, client.attr(ChannelConstant.SOCK).get().get(ConnectionOid.REMOTE_SID).asInt());
		assertEquals(testUuid, client.attr(ChannelConstant.SOCK).get().get(ConnectionOid.REMOTE_UUID).asInt());
		assertNull(client.pipeline().get(SessionRequestHandler.class), "Handler autoremove failed");
	}

}
