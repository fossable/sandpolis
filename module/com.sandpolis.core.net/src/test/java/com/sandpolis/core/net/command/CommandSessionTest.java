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
package com.sandpolis.core.net.command;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.net.ChannelConstant;
import com.sandpolis.core.net.UnitSock;
import com.sandpolis.core.net.handler.ResponseHandler;
import com.sandpolis.core.net.init.AbstractChannelInitializer;
import com.sandpolis.core.net.sock.Sock;
import com.sandpolis.core.proto.net.MSG;
import com.sandpolis.core.proto.util.Result.Outcome;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;

@Disabled
class CommandSessionTest {

	private EmbeddedChannel channel;
	private Sock sock;

	@BeforeAll
	private static void init() {
		ThreadStore.init(config -> {
			config.ephemeral();
			config.defaults.put("net.message.incoming", new UnorderedThreadPoolEventExecutor(4));
		});
	}

	@BeforeEach
	private void setup() {
		channel = new EmbeddedChannel();
		channel.attr(ChannelConstant.CVID).set(10);

		sock = new UnitSock(channel);
		sock.engage(AbstractChannelInitializer.RESPONSE, new ResponseHandler());
	}

	private CommandSession newSession() {
		return new CommandSession(new UnorderedThreadPoolEventExecutor(4), 10, sock, 500);
	}

	@Test
	@DisplayName("Make a simple request")
	void request_1() {
		CommandSession session = newSession();

		AtomicBoolean handlerActivated = new AtomicBoolean();
		session.request(Outcome.newBuilder(), (Outcome response) -> {
			// Check state
			assertFalse(session.isDone());
			assertFalse(session.isSuccess());

			handlerActivated.set(true);
		}, (Outcome response) -> {
			// This handler should never be invoked
			fail();
		});

		// Check state
		assertFalse(session.isDone());
		assertFalse(session.isSuccess());

		// Transfer messages and wait
		MSG.Message rq = channel.readOutbound();
		channel.writeInbound(MSG.Message.newBuilder().setId(rq.getId()).setRsOutcome(Outcome.newBuilder()).build());
		await().atMost(500, MILLISECONDS).untilTrue(handlerActivated);

		// Check state
		assertTrue(session.isDone());
		assertTrue(session.isSuccess());
		assertTrue(session.getNow().getResult());
	}

	@Test
	@DisplayName("Make a nested request")
	void request_2() {
		CommandSession session = newSession();

		AtomicBoolean handler1Activated = new AtomicBoolean();
		AtomicBoolean handler2Activated = new AtomicBoolean();
		session.request(Outcome.newBuilder(), (Outcome response) -> {

			// Check state
			assertFalse(session.isDone());
			assertFalse(session.isSuccess());

			handler1Activated.set(true);

			session.request(Outcome.newBuilder(), (Outcome response2) -> {

				// Check state
				assertFalse(session.isDone());
				assertFalse(session.isSuccess());

				handler2Activated.set(true);
			});
		}, (Outcome response) -> {
			// This handler should never be invoked
			fail();
		});

		// Check state
		assertFalse(session.isDone());
		assertFalse(session.isSuccess());

		// Transfer messages and wait
		MSG.Message rq = channel.readOutbound();
		channel.writeInbound(MSG.Message.newBuilder().setId(rq.getId()).setRsOutcome(Outcome.newBuilder()).build());
		await().atMost(500, MILLISECONDS).untilTrue(handler1Activated);
		rq = channel.readOutbound();
		channel.writeInbound(MSG.Message.newBuilder().setId(rq.getId()).setRsOutcome(Outcome.newBuilder()).build());
		await().atMost(500, MILLISECONDS).untilTrue(handler2Activated);

		// Check state
		assertTrue(session.isDone());
		assertTrue(session.isSuccess());
		assertTrue(session.getNow().getResult());
	}

	@Test
	@DisplayName("Abort the session")
	void abort_1() {
		CommandSession session = newSession();

		// Check state
		assertFalse(session.isDone());
		assertFalse(session.isSuccess());

		AtomicBoolean handlerActivated = new AtomicBoolean();
		session.request(Outcome.newBuilder(), (Outcome response) -> {
			session.abort("Something failed");
			handlerActivated.set(true);
		});

		MSG.Message rq = channel.readOutbound();
		channel.writeInbound(MSG.Message.newBuilder().setId(rq.getId()).setRsOutcome(Outcome.newBuilder()).build());
		await().atMost(500, MILLISECONDS).untilTrue(handlerActivated);

		// Check state
		assertTrue(session.isDone());
		assertFalse(session.isSuccess());

	}

	@Test
	@DisplayName("Throw an exception from a message handler")
	void test_exception_1() {
		CommandSession session = newSession();

		session.request(Outcome.newBuilder(), (Outcome response) -> {
			throw new Exception("expected exception");
		});

		MSG.Message rq = channel.readOutbound();
		channel.writeInbound(MSG.Message.newBuilder().setId(rq.getId()).setRsOutcome(Outcome.newBuilder()).build());
		await().atMost(500, MILLISECONDS).until(() -> session.isDone());

		// Check state
		assertFalse(session.isSuccess());
		assertEquals("expected exception", session.cause().getMessage());
	}

}
