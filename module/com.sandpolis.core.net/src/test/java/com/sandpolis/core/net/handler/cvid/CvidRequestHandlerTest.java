/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
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

import com.sandpolis.core.net.ChannelConstant;
import com.sandpolis.core.net.handler.cvid.CvidRequestHandler;
import com.sandpolis.core.net.handler.cvid.AbstractCvidHandler.CvidHandshakeCompletionEvent;
import com.sandpolis.core.proto.net.MCCvid.RQ_Cvid;
import com.sandpolis.core.proto.net.MCCvid.RS_Cvid;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;
import com.sandpolis.core.util.IDUtil;

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
		HANDLER.handshake(client, Instance.CLIENT, InstanceFlavor.MEGA, "testuuid");

		Message msg = client.readOutbound();
		RQ_Cvid rq = msg.getRqCvid();

		assertTrue(rq != null);
		assertEquals(Instance.CLIENT, rq.getInstance());
		assertEquals(InstanceFlavor.MEGA, rq.getInstanceFlavor());
		assertEquals("testuuid", rq.getUuid());
	}

	@Test
	@DisplayName("Receive an invalid response")
	void testReceiveIncorrect() {
		client.writeInbound(Message.newBuilder().setRsCvid(RS_Cvid.newBuilder()).build());

		await().atMost(1000, TimeUnit.MILLISECONDS).until(() -> event != null);
		assertFalse(event.isSuccess());
		assertNull(client.pipeline().get(CvidRequestHandler.class), "Handler autoremove failed");
	}

	@Test
	@DisplayName("Receive a valid response")
	void testReceiveCorrect() {
		client.writeInbound(Message.newBuilder().setRsCvid(RS_Cvid.newBuilder()
				.setCvid(IDUtil.CVID.cvid(Instance.CLIENT)).setServerCvid(123).setServerUuid("testuuid")).build());

		await().atMost(1000, TimeUnit.MILLISECONDS).until(() -> event != null);
		assertTrue(event.isSuccess());

		assertEquals(123, client.attr(ChannelConstant.CVID).get().intValue());
		assertEquals("testuuid", client.attr(ChannelConstant.UUID).get());
		assertNull(client.pipeline().get(CvidRequestHandler.class), "Handler autoremove failed");
	}

}