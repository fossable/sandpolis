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

import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;

import com.sandpolis.core.instance.Instance;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.proto.net.MCCvid.RQ_Cvid;
import com.sandpolis.core.proto.net.MCCvid.RS_Cvid;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.util.IDUtil;

import io.netty.channel.embedded.EmbeddedChannel;

public class CvidRequestHandlerTest {

	private static final CvidRequestHandler clientHandler = new CvidRequestHandler();
	private EmbeddedChannel client;

	@Before
	public void setup() {
		client = new EmbeddedChannel();
		client.pipeline().addLast("cvid", clientHandler);
		client.attr(Sock.CVID_HANDLER_KEY).set(new CompletableFuture<>());
	}

	@Test
	public void testInitiate() {
		clientHandler.initiateHandshake(client, Instance.CLIENT, "testuuid");
		assertFalse(client.attr(Sock.CVID_HANDLER_KEY).get().isDone());

		Message msg = client.readOutbound();
		RQ_Cvid rq = msg.getRqCvid();

		assertTrue(rq != null);
		assertEquals(IDUtil.CVID.getIID(Instance.CLIENT), rq.getIid());
		assertEquals("testuuid", rq.getUuid());
	}

	@Test
	public void testReceiveIncorrect() {
		assertNotNull(client.pipeline().get("cvid"));
		assertFalse(client.attr(Sock.CVID_HANDLER_KEY).get().isDone());
		client.writeInbound(Message.newBuilder().setRsCvid(RS_Cvid.newBuilder()).build());
		assertTrue(client.attr(Sock.CVID_HANDLER_KEY).get().isDone());
		assertTrue(client.attr(Sock.CVID_HANDLER_KEY).get().isCompletedExceptionally());
		assertNull(client.pipeline().get("cvid"));
	}

	@Test
	public void testReceiveCorrect() {
		assertNotNull(client.pipeline().get("cvid"));
		assertFalse(client.attr(Sock.CVID_HANDLER_KEY).get().isDone());
		client.writeInbound(Message.newBuilder().setRsCvid(RS_Cvid.newBuilder()
				.setCvid(IDUtil.CVID.cvid(Instance.CLIENT)).setServerCvid(123).setServerUuid("testuuid")).build());
		assertTrue(client.attr(Sock.CVID_HANDLER_KEY).get().isDone());
		assertFalse(client.attr(Sock.CVID_HANDLER_KEY).get().isCompletedExceptionally());

		assertEquals(123, client.attr(Sock.CVID_KEY).get().intValue());
		assertEquals("testuuid", client.attr(Sock.UUID_KEY).get());
		assertNull(client.pipeline().get("cvid"));
	}

}
