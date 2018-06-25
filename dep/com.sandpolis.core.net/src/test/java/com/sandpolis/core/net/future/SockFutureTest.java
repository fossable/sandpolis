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
package com.sandpolis.core.net.future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.sandpolis.core.net.Sock;
import com.sandpolis.core.test.TestClientInitializer;
import com.sandpolis.core.test.TestServerInitializer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;

public class SockFutureTest {

	@Test
	public void testGet() throws InterruptedException, ExecutionException {
		EmbeddedChannel server = new EmbeddedChannel(new TestServerInitializer());
		server.bind(new InetSocketAddress(6000));

		EmbeddedChannel client = new EmbeddedChannel(new TestClientInitializer());
		SockFuture sf = new SockFuture(client.connect(new InetSocketAddress("127.0.0.1", 6000)));
		assertFalse(sf.isDone());

		// Move the handshake messages manually
		ByteBuf msg = client.readOutbound();
		server.writeInbound(msg);
		msg = server.readOutbound();
		client.writeInbound(msg);

		Sock sock = sf.get();
		assertTrue(sf.isDone());
		assertTrue(sf.isSuccess());
		assertNull(sf.cause());

		assertNotNull(sock);
		assertEquals(client, sock.channel());

		server.close();
		client.close();
	}

}
