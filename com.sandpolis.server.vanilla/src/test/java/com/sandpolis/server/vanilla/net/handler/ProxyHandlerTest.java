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
package com.sandpolis.server.vanilla.net.handler;

import static com.sandpolis.core.net.ChannelConstant.CVID;
import static com.sandpolis.core.net.store.connection.ConnectionStore.ConnectionStore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.net.exception.InvalidMessageException;
import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.proto.net.Message.MSG.PayloadCase;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

class ProxyHandlerTest {

	/**
	 * A channel that contains a {@link ProxyHandler}.
	 */
	private final EmbeddedChannel proxy = new EmbeddedChannel(new ProtobufVarint32FrameDecoder(),
			new ProxyHandler(2000), new ProtobufDecoder(MSG.getDefaultInstance()));

	/**
	 * A channel that can be used to encode {@link MSG}s into {@link ByteBuf}s.
	 */
	private final EmbeddedChannel encoder = new EmbeddedChannel(new ProtobufVarint32LengthFieldPrepender(),
			new ProtobufEncoder());

	@BeforeAll
	static void setup() {
		ConnectionStore.init(config -> {
			config.ephemeral();
		});
	}

	@Test
	@DisplayName("Check that important field numbers will never change")
	void messageFieldNumbers() {
		assertEquals(1, MSG.TO_FIELD_NUMBER);
		assertEquals(2, MSG.FROM_FIELD_NUMBER);
		assertEquals(3, MSG.ID_FIELD_NUMBER);
	}

	@Test
	@DisplayName("Allow empty messages to pass through the ProxyHandler")
	void emptyPassthrough() {
		assertPassthrough(MSG.newBuilder().build());
	}

	@Test
	@DisplayName("Disallow messages with negative CVIDs")
	void negativeCvid() {
		assertThrows(CorruptedFrameException.class,
				() -> proxy.writeInbound(encode(MSG.newBuilder().setTo(-1234).build())));
		assertThrows(CorruptedFrameException.class,
				() -> proxy.writeInbound(encode(MSG.newBuilder().setTo(-1).build())));

		// Ensure no messages were passed through
		assertTrue(proxy.inboundMessages().isEmpty());
	}

	@Test
	@DisplayName("Ensure messages addressed to this instance are passed through the ProxyHandler")
	void simplePassthrough() {
		proxy.attr(CVID).set(1234);
		assertPassthrough(MSG.newBuilder().setTo(2000).setFrom(1234).build());
		assertPassthrough(MSG.newBuilder().setFrom(1234).build());
	}

	@Test
	@DisplayName("Ensure the 'from' field is specified if the message intends to be routed")
	void checkFromSpecified() {
		assertThrows(InvalidMessageException.class,
				() -> proxy.writeInbound(encode(MSG.newBuilder().setTo(1234).build())));

		// Ensure no messages were passed through
		assertTrue(proxy.inboundMessages().isEmpty());
	}

	@Test
	@DisplayName("Ensure the 'from' field matches the channel CVID")
	void spoofDetection() {
		proxy.attr(CVID).set(4321);
		assertThrows(InvalidMessageException.class,
				() -> proxy.writeInbound(encode(MSG.newBuilder().setTo(600).setFrom(1234).build())));

		// Ensure no messages were passed through
		assertTrue(proxy.inboundMessages().isEmpty());

		// Messages intended for the server with spoofed 'from' fields must be allowed
		assertPassthrough(MSG.newBuilder().setTo(2000).setFrom(1234).build());
	}

	@Test
	@DisplayName("Ensure the ProxyHandler sends a message if the routing target is missing")
	void endpointClosedDetection() {
		proxy.attr(CVID).set(4321);

		proxy.writeInbound(encode(MSG.newBuilder().setTo(1234).setFrom(4321).build()));
		assertEquals(PayloadCase.EV_ENDPOINT_CLOSED, ((MSG) proxy.readOutbound()).getPayloadCase());

		// Ensure no messages were passed through
		assertTrue(proxy.inboundMessages().isEmpty());
	}

	/**
	 * Assert that the given message passes through the {@link ProxyHandler}
	 * unchanged.
	 *
	 * @param msgToPass The message to pass through
	 */
	private void assertPassthrough(MSG msgToPass) {
		assertTrue(proxy.writeInbound(encode(msgToPass)));
		assertEquals(msgToPass, proxy.readInbound());
	}

	/**
	 * Convert a {@link MSG} to a {@link ByteBuf}.
	 *
	 * @param message The message to encode
	 * @return A new ByteBuf
	 */
	private ByteBuf encode(MSG message) {
		assertTrue(encoder.writeOutbound(message));
		return encoder.readOutbound();
	}
}
