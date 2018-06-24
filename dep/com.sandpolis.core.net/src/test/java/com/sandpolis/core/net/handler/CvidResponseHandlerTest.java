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

public class CvidResponseHandlerTest {

	private static final CvidResponseHandler serverHandler = new CvidResponseHandler(123, "testuuid");
	private EmbeddedChannel server;

	@Before
	public void setup() {
		server = new EmbeddedChannel();
		server.pipeline().addLast("cvid", serverHandler);
		server.attr(Sock.CVID_HANDLER_KEY).set(new CompletableFuture<>());
	}

	@Test
	public void testReceiveIncorrect() {
		assertNotNull(server.pipeline().get("cvid"));
		assertFalse(server.attr(Sock.CVID_HANDLER_KEY).get().isDone());
		server.writeInbound(Message.newBuilder()
				.setRqCvid(RQ_Cvid.newBuilder().setIid(IDUtil.CVID.getIID(Instance.SERVER)).setUuid("testuuid2"))
				.build());
		assertTrue(server.attr(Sock.CVID_HANDLER_KEY).get().isDone());
		assertTrue(server.attr(Sock.CVID_HANDLER_KEY).get().isCompletedExceptionally());
		assertNull(server.pipeline().get("cvid"));
	}

	@Test
	public void testReceiveCorrect() {
		assertNotNull(server.pipeline().get("cvid"));
		assertFalse(server.attr(Sock.CVID_HANDLER_KEY).get().isDone());
		server.writeInbound(Message.newBuilder()
				.setRqCvid(RQ_Cvid.newBuilder().setIid(IDUtil.CVID.getIID(Instance.CLIENT)).setUuid("testuuid2"))
				.build());
		assertTrue(server.attr(Sock.CVID_HANDLER_KEY).get().isDone());
		assertFalse(server.attr(Sock.CVID_HANDLER_KEY).get().isCompletedExceptionally());

		assertEquals(Instance.CLIENT, IDUtil.CVID.extractInstance(server.attr(Sock.CVID_KEY).get()));
		assertEquals("testuuid2", server.attr(Sock.UUID_KEY).get());
		assertNull(server.pipeline().get("cvid"));

		Message msg = server.readOutbound();
		RS_Cvid rs = msg.getRsCvid();

		assertEquals(Instance.CLIENT, IDUtil.CVID.extractInstance(rs.getCvid()));
		assertEquals(123, rs.getServerCvid());
		assertEquals("testuuid", rs.getServerUuid());

	}

}
