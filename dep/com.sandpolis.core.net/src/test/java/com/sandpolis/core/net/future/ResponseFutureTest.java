/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.store.thread.ThreadStore;
import com.sandpolis.core.net.exception.InvalidMessageException;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.util.Result.Outcome;

import io.netty.util.concurrent.GlobalEventExecutor;

public class ResponseFutureTest {

	@BeforeEach
	public void setup() {
		ThreadStore.register(GlobalEventExecutor.INSTANCE, "net.message.incoming");
	}

	@Test
	public void testResponseSuccess() throws InterruptedException, ExecutionException {
		MessageFuture msgFuture = new MessageFuture();
		ResponseFuture<Outcome> response = new ResponseFuture<>(msgFuture);
		assertFalse(response.isDone());

		msgFuture.setSuccess(Message.newBuilder().setRsOutcome(Outcome.newBuilder().setResult(true)).build());

		response.sync();
		assertTrue(response.isSuccess());
		assertTrue(response.get().getResult());
	}

	@Test
	public void testResponseInvalid() throws InterruptedException, ExecutionException {
		MessageFuture msgFuture = new MessageFuture();
		ResponseFuture<Outcome> response = new ResponseFuture<>(msgFuture);
		assertFalse(response.isDone());

		msgFuture.setSuccess(Message.newBuilder().build());

		assertThrows(InvalidMessageException.class, () -> response.sync());
	}

}
