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
import org.s7s.core.protocol.Message.MSG;
import org.s7s.core.protocol.Session.RQ_Session;
import org.s7s.core.protocol.Session.RS_Session;
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
class SessionResponseHandlerTest {

	private static final SessionResponseHandler HANDLER = new SessionResponseHandler();

	private EmbeddedChannel server;
	private SessionHandshakeCompletionEvent event;

	@BeforeEach
	void setup() {
		server = new EmbeddedChannel();
		event = null;

		server.pipeline().addFirst(new ChannelInboundHandlerAdapter() {
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
		server.pipeline().addFirst(HANDLER);
	}

	@Test
	@DisplayName("Receive an invalid response")
	void testReceiveIncorrect() {
		var testUuid = randomUUID().toString();

		server.writeInbound(S7SMsg.rq()
				.pack(RQ_Session.newBuilder().setInstanceType(InstanceType.SERVER).setInstanceUuid(testUuid)).build());

		await().atMost(1000, TimeUnit.MILLISECONDS).until(() -> event != null);
		assertFalse(event.success);
		assertNull(server.pipeline().get(SessionResponseHandler.class), "Handler autoremove failed");
	}

	@Test
	@DisplayName("Receive a valid response")
	void testReceiveCorrect() throws InvalidProtocolBufferException {
		var testUuid = randomUUID().toString();

		server.writeInbound(S7SMsg.rq()
				.pack(RQ_Session.newBuilder().setInstanceType(InstanceType.CLIENT).setInstanceUuid(testUuid)));

		await().atMost(1000, TimeUnit.MILLISECONDS).until(() -> event != null);
		assertTrue(event.success);

		assertEquals(InstanceType.CLIENT,
				S7SSessionID.of(server.attr(ChannelConstant.SOCK).get().get(ConnectionOid.REMOTE_SID).asInt()));
		assertEquals(testUuid, server.attr(ChannelConstant.SOCK).get().get(ConnectionOid.REMOTE_UUID).asString());
		assertNull(server.pipeline().get(SessionResponseHandler.class), "Handler autoremove failed");

		MSG msg = server.readOutbound();
		RS_Session rs = S7SMsg.of(server.readOutbound()).unpack(RS_Session.class);

		assertEquals(InstanceType.CLIENT, S7SSessionID.of(rs.getInstanceSid()).instanceType());
		assertFalse(rs.getServerUuid().isEmpty());

	}

}
