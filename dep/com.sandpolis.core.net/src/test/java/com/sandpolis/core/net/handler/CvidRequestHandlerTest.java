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
package com.sandpolis.core.net.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sandpolis.core.net.init.ChannelConstant;
import com.sandpolis.core.proto.net.MCCvid.RQ_Cvid;
import com.sandpolis.core.proto.net.MCCvid.RS_Cvid;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.util.IDUtil;

import io.netty.channel.DefaultEventLoop;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.DefaultPromise;

public class CvidRequestHandlerTest {

	private static final CvidRequestHandler clientHandler = new CvidRequestHandler();
	private EmbeddedChannel client;

	@Before
	public void setup() {
		client = new EmbeddedChannel();
		client.pipeline().addLast("cvid", clientHandler);
		client.attr(ChannelConstant.HANDLER_CVID).set(new DefaultPromise<>(new DefaultEventLoop()));
	}

	@Test
	public void testInitiate() {
		clientHandler.handshake(client, Instance.CLIENT, "testuuid");
		assertFalse(client.attr(ChannelConstant.HANDLER_CVID).get().isDone());

		Message msg = client.readOutbound();
		RQ_Cvid rq = msg.getRqCvid();

		assertTrue(rq != null);
		assertEquals(Instance.CLIENT, rq.getInstance());
		assertEquals("testuuid", rq.getUuid());
	}

	@Test
	public void testReceiveIncorrect() {
		assertNotNull(client.pipeline().get("cvid"));
		assertFalse(client.attr(ChannelConstant.HANDLER_CVID).get().isDone());
		client.writeInbound(Message.newBuilder().setRsCvid(RS_Cvid.newBuilder()).build());
		assertTrue(client.attr(ChannelConstant.HANDLER_CVID).get().isDone());
		assertFalse(client.attr(ChannelConstant.HANDLER_CVID).get().isSuccess());
		assertNull(client.pipeline().get("cvid"));
	}

	@Test
	public void testReceiveCorrect() {
		assertNotNull(client.pipeline().get("cvid"));
		assertFalse(client.attr(ChannelConstant.HANDLER_CVID).get().isDone());
		client.writeInbound(Message.newBuilder().setRsCvid(RS_Cvid.newBuilder()
				.setCvid(IDUtil.CVID.cvid(Instance.CLIENT)).setServerCvid(123).setServerUuid("testuuid")).build());
		assertTrue(client.attr(ChannelConstant.HANDLER_CVID).get().isDone());
		assertTrue(client.attr(ChannelConstant.HANDLER_CVID).get().isSuccess());

		assertEquals(123, client.attr(ChannelConstant.CVID).get().intValue());
		assertEquals("testuuid", client.attr(ChannelConstant.UUID).get());
		assertNull(client.pipeline().get("cvid"));
	}

}
