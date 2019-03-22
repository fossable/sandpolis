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
package com.sandpolis.server.net.handler;

import static com.sandpolis.core.net.init.ChannelConstant.CVID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.Signaler;
import com.sandpolis.core.net.exception.InvalidMessageException;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.net.MSG.Message.MsgOneofCase;

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
			new ProxyHandler(2000), new ProtobufDecoder(Message.getDefaultInstance()));

	/**
	 * A channel that can be used to encode {@link Message}s into {@link ByteBuf}s.
	 */
	private final EmbeddedChannel encoder = new EmbeddedChannel(new ProtobufVarint32LengthFieldPrepender(),
			new ProtobufEncoder());

	@Test
	@DisplayName("Check that important field numbers will never change")
	void messageFieldNumbers() {
		assertEquals(1, Message.TO_FIELD_NUMBER);
		assertEquals(2, Message.FROM_FIELD_NUMBER);
		assertEquals(3, Message.ID_FIELD_NUMBER);
	}

	@Test
	@DisplayName("Allow empty messages to pass through the ProxyHandler")
	void emptyPassthrough() {
		assertPassthrough(Message.newBuilder().build());
	}

	@Test
	@DisplayName("Disallow messages with negative CVIDs")
	void negativeCvid() {
		assertThrows(CorruptedFrameException.class,
				() -> proxy.writeInbound(encode(Message.newBuilder().setTo(-1234).build())));
		assertThrows(CorruptedFrameException.class,
				() -> proxy.writeInbound(encode(Message.newBuilder().setTo(-1).build())));

		// Ensure no messages were passed through
		assertTrue(proxy.inboundMessages().isEmpty());
	}

	@Test
	@DisplayName("Ensure messages addressed to this instance are passed through the ProxyHandler")
	void simplePassthrough() {
		proxy.attr(CVID).set(1234);
		assertPassthrough(Message.newBuilder().setTo(2000).setFrom(1234).build());
		assertPassthrough(Message.newBuilder().setFrom(1234).build());
	}

	@Test
	@DisplayName("Ensure the 'from' field is specified if the message intends to be routed")
	void checkFromSpecified() {
		assertThrows(InvalidMessageException.class,
				() -> proxy.writeInbound(encode(Message.newBuilder().setTo(1234).build())));

		// Ensure no messages were passed through
		assertTrue(proxy.inboundMessages().isEmpty());
	}

	@Test
	@DisplayName("Ensure the 'from' field matches the channel CVID")
	void spoofDetection() {
		proxy.attr(CVID).set(4321);
		assertThrows(InvalidMessageException.class,
				() -> proxy.writeInbound(encode(Message.newBuilder().setTo(600).setFrom(1234).build())));

		// Ensure no messages were passed through
		assertTrue(proxy.inboundMessages().isEmpty());

		// Messages intended for the server with spoofed 'from' fields must be allowed
		assertPassthrough(Message.newBuilder().setTo(2000).setFrom(1234).build());
	}

	@Test
	@DisplayName("Ensure the ProxyHandler sends a message if the routing target is missing")
	void endpointClosedDetection() {
		proxy.attr(CVID).set(4321);

		proxy.writeInbound(encode(Message.newBuilder().setTo(1234).setFrom(4321).build()));
		assertEquals(MsgOneofCase.EV_ENDPOINT_CLOSED, ((Message) proxy.readOutbound()).getMsgOneofCase());

		// Ensure no messages were passed through
		assertTrue(proxy.inboundMessages().isEmpty());
	}

	/**
	 * Assert that the given message passes through the {@link ProxyHandler}
	 * unchanged.
	 * 
	 * @param msgToPass The message to pass through
	 */
	private void assertPassthrough(Message msgToPass) {
		assertTrue(proxy.writeInbound(encode(msgToPass)));
		assertEquals(msgToPass, proxy.readInbound());
	}

	/**
	 * Convert a {@link Message} to a {@link ByteBuf}.
	 * 
	 * @param message The message to encode
	 * @return A new ByteBuf
	 */
	private ByteBuf encode(Message message) {
		assertTrue(encoder.writeOutbound(message));
		return encoder.readOutbound();
	}

	@BeforeAll
	private static void init() {
		Signaler.init(Executors.newSingleThreadExecutor());
	}
}
