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

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.sandpolis.core.instance.store.thread.ThreadStore;
import com.sandpolis.core.net.exception.InvalidMessageException;
import com.sandpolis.core.proto.net.MSG.Message;

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
public class ResponseFuture<E> extends DefaultPromise<E> {

	/**
	 * Construct a new {@link ResponseFuture} that listens on the given
	 * {@link MessageFuture}.
	 * 
	 * @param future
	 *            The {@link MessageFuture} that will be notified when the response
	 *            arrives
	 */
	public ResponseFuture(MessageFuture future) {
		this(ThreadStore.get("net.message.incoming"), future);
	}

	/**
	 * Construct a new {@link ResponseFuture} that listens on the given
	 * {@link MessageFuture}.
	 * 
	 * @param executor
	 *            The {@link EventExecutor} that will be used to execute listeners
	 * @param future
	 *            The {@link MessageFuture} that will be notified when the response
	 *            arrives
	 */
	@SuppressWarnings("unchecked")
	public ResponseFuture(EventExecutor executor, MessageFuture future) {
		super(executor);

		future.addListener((MessageFuture messageFuture) -> {
			E response;

			if (!messageFuture.isSuccess()) {
				setFailure(messageFuture.cause());
				return;
			}

			Message message = messageFuture.get();
			FieldDescriptor oneof = message.getOneofFieldDescriptor(Message.getDescriptor().getOneofs().get(0));

			if (oneof == null) {
				setFailure(new InvalidMessageException("Invalid response"));
				return;
			}

			try {
				response = (E) message.getField(oneof);
			} catch (ClassCastException e) {
				setFailure(new InvalidMessageException("Invalid response"));
				return;
			}

			if (response == null) {
				setFailure(new InvalidMessageException("Invalid response"));
				return;
			}

			setSuccess(response);
		});
	}
}
