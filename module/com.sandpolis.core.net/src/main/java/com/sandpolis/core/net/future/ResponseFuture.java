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

import java.util.concurrent.ExecutionException;

import com.google.protobuf.Message;
import com.sandpolis.core.instance.PoolConstant.net;
import com.sandpolis.core.instance.store.thread.ThreadStore;
import com.sandpolis.core.net.command.CommandFuture.MessageHandler;
import com.sandpolis.core.net.exception.InvalidMessageException;
import com.sandpolis.core.util.ProtoUtil;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;

/**
 * A {@link Future} that allows a {@link Message} response to be asynchronously
 * handled. This class extracts the response type from the received
 * {@link Message}, so use cases that require the message ID should use
 * {@link MessageFuture} instead.<br>
 * <br>
 * 
 * An extremely convenient usage pattern is to pass an executor for a GUI update
 * thread to the constructor so the GUI can be updated by message responses.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class ResponseFuture<E extends Message> extends DefaultPromise<Message> {

	/**
	 * Construct a new {@link ResponseFuture} that listens on the given
	 * {@link MessageFuture}.
	 * 
	 * @param future The {@link MessageFuture} that will be notified when the
	 *               response arrives
	 */
	public ResponseFuture(MessageFuture future) {
		this(ThreadStore.get(net.message.incoming), future);
	}

	/**
	 * Construct a new {@link ResponseFuture} that listens on the given
	 * {@link MessageFuture}.
	 * 
	 * @param executor The {@link EventExecutor} that will be used to execute
	 *                 listeners
	 * @param future   The {@link MessageFuture} that will be notified when the
	 *                 response arrives
	 */
	public ResponseFuture(EventExecutor executor, MessageFuture future) {
		super(executor);

		future.addListener((MessageFuture messageFuture) -> {

			if (!messageFuture.isSuccess()) {
				setFailure(messageFuture.cause());
				return;
			}

			var message = (Message) ProtoUtil.getPayload(messageFuture.get());

			if (message == null) {
				setFailure(new InvalidMessageException("Empty response"));
				return;
			}

			setSuccess(message);
		});
	}

	@SuppressWarnings("unchecked")
	public E getExpected() throws InterruptedException, ExecutionException {
		return (E) get();
	}

	/**
	 * Add the given handler to this future.
	 * 
	 * @param handler The message handler
	 * @return {@code this}
	 */
	@SuppressWarnings("unchecked")
	public <M extends Message> ResponseFuture<E> addHandler(MessageHandler<M> handler) {
		addListener(f -> {
			try {
				if (f.isSuccess())
					handler.handle((M) f.getNow());
			} catch (ClassCastException e) {
				// Ignore
			}
		});

		return this;
	}

}
