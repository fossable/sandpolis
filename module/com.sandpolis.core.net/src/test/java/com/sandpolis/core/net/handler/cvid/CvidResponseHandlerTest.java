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
import com.sandpolis.core.net.handler.cvid.AbstractCvidHandler.CvidHandshakeCompletionEvent;
import com.sandpolis.core.net.util.CvidUtil;
import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.proto.net.MsgCvid.RQ_Cvid;
import com.sandpolis.core.proto.net.MsgCvid.RS_Cvid;
import com.sandpolis.core.proto.util.Platform.Instance;

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
				.setRqCvid(RQ_Cvid.newBuilder().setInstance(Instance.SERVER).setUuid("testuuid2")).build());

		await().atMost(1000, TimeUnit.MILLISECONDS).until(() -> event != null);
		assertFalse(event.isSuccess());
		assertNull(server.pipeline().get(CvidResponseHandler.class), "Handler autoremove failed");
	}

	@Test
	@DisplayName("Receive a valid response")
	void testReceiveCorrect() {
		server.writeInbound(MSG.newBuilder()
				.setRqCvid(RQ_Cvid.newBuilder().setInstance(Instance.CLIENT).setUuid("testuuid2")).build());

		await().atMost(1000, TimeUnit.MILLISECONDS).until(() -> event != null);
		assertTrue(event.isSuccess());

		assertEquals(Instance.CLIENT, CvidUtil.extractInstance(server.attr(ChannelConstant.CVID).get()));
		assertEquals("testuuid2", server.attr(ChannelConstant.UUID).get());
		assertNull(server.pipeline().get(CvidResponseHandler.class), "Handler autoremove failed");

		MSG msg = server.readOutbound();
		RS_Cvid rs = msg.getRsCvid();

		assertEquals(Instance.CLIENT, CvidUtil.extractInstance(rs.getCvid()));
		assertFalse(rs.getServerUuid().isEmpty());

	}

}
