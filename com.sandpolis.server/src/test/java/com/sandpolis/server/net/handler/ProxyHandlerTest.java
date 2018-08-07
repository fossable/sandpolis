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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sandpolis.core.net.exception.InvalidMessageException;
import com.sandpolis.core.net.init.ChannelConstant;
import com.sandpolis.core.proto.net.MCLogin.RQ_Login;
import com.sandpolis.core.proto.net.MCUser.RQ_AddUser;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.net.MSG.Message.MsgOneofCase;
import com.sandpolis.core.util.RandUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

class ProxyHandlerTest {

	private EmbeddedChannel proxy = new EmbeddedChannel(new ProtobufVarint32FrameDecoder(), new ProxyHandler(),
			new ProtobufDecoder(Message.getDefaultInstance()));
	private EmbeddedChannel encoder = new EmbeddedChannel(new ProtobufVarint32LengthFieldPrepender(),
			new ProtobufEncoder());

	/**
	 * Messages that should be routed
	 */
	private Message[] routing = new Message[] { Message.newBuilder().setTo(14).setFrom(12).build(),
			Message.newBuilder().setTo(14).setFrom(12).setId(19)
					.setRqAddUser(RQ_AddUser.newBuilder().setUser(RandUtil.nextAlphabetic(2048))).build() };

	/**
	 * Messages that should pass through the proxy unchanged
	 */
	private Message[] validPassthrough = new Message[] { Message.newBuilder().build(),
			Message.newBuilder().setRqLogin(RQ_Login.newBuilder().setUsername("Test")).build() };

	@Test
	void testSpoofDetection() {

		for (Message message : routing) {
			// Ensure from field is wrong
			proxy.attr(ChannelConstant.CVID).set(message.getFrom() + 1);

			assertThrows(InvalidMessageException.class, () -> proxy.writeInbound(encode(message)));
		}
	}

	@Test
	void testRoutingWithMissingTarget() {
		for (Message message : routing) {
			// Don't test spoofing detection here
			proxy.attr(ChannelConstant.CVID).set(message.getFrom());

			proxy.writeInbound(encode(message));
			assertEquals(MsgOneofCase.EV_ENDPOINT_CLOSED, ((Message) proxy.readOutbound()).getMsgOneofCase());
		}
	}

	@Test
	void testPassthrough() {
		for (Message message : validPassthrough)
			testPassthrough(message);
	}

	@Test
	void testMessageFieldNumbers() {

		// Test field numbers that must stay constant for the proxy handler
		assertEquals(1, Message.TO_FIELD_NUMBER);
		assertEquals(2, Message.FROM_FIELD_NUMBER);
		assertEquals(3, Message.ID_FIELD_NUMBER);
	}

	/**
	 * Ensure that a message passes through safely.
	 */
	private void testPassthrough(Message passthrough) {
		ByteBuf buf = encode(passthrough);

		proxy.writeInbound(buf);
		assertEquals(passthrough, proxy.readInbound());
	}

	/**
	 * Convert a message into a ByteBuf.
	 */
	private ByteBuf encode(Message message) {
		assertTrue(encoder.writeOutbound(message));
		return encoder.readOutbound();
	}

}
