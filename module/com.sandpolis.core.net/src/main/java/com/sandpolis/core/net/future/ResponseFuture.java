//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.core.net.future;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;

import java.util.concurrent.ExecutionException;

import com.google.protobuf.Message;
import com.sandpolis.core.net.command.CommandFuture.MessageHandler;
import com.sandpolis.core.net.exception.InvalidMessageException;
import com.sandpolis.core.net.util.ProtoUtil;

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
		this(ThreadStore.get("net.message.incoming"), future);
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
