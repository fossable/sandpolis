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
package com.sandpolis.core.net.future;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.net.exception.InvalidMessageException;
import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.proto.net.MsgCvid.RQ_Cvid;
import com.sandpolis.core.proto.net.MsgCvid.RS_Cvid;
import com.sandpolis.core.proto.util.Result.Outcome;

import io.netty.util.concurrent.GlobalEventExecutor;

class ResponseFutureTest {

	@BeforeEach
	private void setup() {
		ThreadStore.init(config -> {
			config.ephemeral();
			config.defaults.put("net.message.incoming", GlobalEventExecutor.INSTANCE);
		});
	}

	@Test
	@DisplayName("Receive the correct response")
	void testResponse_1() throws InterruptedException, ExecutionException {
		MessageFuture msgFuture = new MessageFuture();
		ResponseFuture<Outcome> response = new ResponseFuture<>(msgFuture);
		assertFalse(response.isDone());

		msgFuture.setSuccess(MSG.newBuilder().setRsOutcome(Outcome.newBuilder().setResult(true)).build());

		response.sync();
		assertTrue(response.isSuccess());
		assertTrue(response.getExpected().getResult());
	}

	@Test
	@DisplayName("Try to receive an empty message")
	void testResponse_2() throws InterruptedException, ExecutionException {
		MessageFuture msgFuture = new MessageFuture();
		ResponseFuture<Outcome> response = new ResponseFuture<>(msgFuture);
		assertFalse(response.isDone());

		msgFuture.setSuccess(MSG.newBuilder().build());

		assertThrows(InvalidMessageException.class, () -> response.sync());
		assertTrue(!response.isSuccess());
	}

	@Test
	@DisplayName("Check response handlers")
	void testResponse_3() throws InterruptedException {
		MessageFuture msgFuture = new MessageFuture();
		ResponseFuture<RS_Cvid> response = new ResponseFuture<>(msgFuture);
		assertFalse(response.isDone());

		response.addHandler((Outcome rs) -> {
			fail();
		});
		response.addHandler((RS_Cvid rs) -> {
			fail();
		});

		AtomicBoolean called1 = new AtomicBoolean();
		response.addHandler((RQ_Cvid rs) -> {
			called1.set(true);
			synchronized (called1) {
				called1.notify();
			}
		});

		msgFuture.setSuccess(MSG.newBuilder().setRqCvid(RQ_Cvid.newBuilder()).build());

		response.addHandler((Outcome rs) -> {
			fail();
		});

		AtomicBoolean called2 = new AtomicBoolean();
		response.addHandler((RQ_Cvid rs) -> {
			called2.set(true);
			synchronized (called2) {
				called2.notify();
			}
		});

		// Wait for handlers to finish
		synchronized (called1) {
			called1.wait(1000);
		}
		synchronized (called2) {
			called2.wait(1000);
		}

		assertTrue(called1.get());
		assertTrue(called2.get());
	}

	@Test
	@DisplayName("Receive an outcome instead of the expected type")
	void testResponse_4() throws InterruptedException, ExecutionException {
		MessageFuture msgFuture = new MessageFuture();
		ResponseFuture<RS_Cvid> response = new ResponseFuture<>(msgFuture);
		assertFalse(response.isDone());

		msgFuture.setSuccess(MSG.newBuilder().setRsOutcome(Outcome.newBuilder().setResult(false)).build());

		response.sync();
		assertThrows(ClassCastException.class, () -> {
			@SuppressWarnings("unused")
			RS_Cvid rs = response.getExpected();
		});
		assertTrue(response.isSuccess());
		assertFalse(((Outcome) response.get()).getResult());
	}

}
