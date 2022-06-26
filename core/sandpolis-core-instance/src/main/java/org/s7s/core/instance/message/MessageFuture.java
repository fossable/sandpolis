//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.message;

import static org.s7s.core.instance.thread.ThreadStore.ThreadStore;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.s7s.core.protocol.Message.MSG;
import org.s7s.core.instance.util.S7SMsg;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;

/**
 * A {@link MessageFuture} is given to a thread waiting for a message (most
 * likely a response of some sort). If the desired message arrives before the
 * thread times out, the {@link MessageFuture} gives the waiting thread access
 * to the message.
 *
 * @author cilki
 * @since 4.0.0
 */
public class MessageFuture extends DefaultPromise<MSG> {

	/**
	 * Construct a {@link MessageFuture} that only completes if the message arrives.
	 */
	public MessageFuture() {
		// Don't bother setting a timer
		super(ThreadStore.get("net.message.incoming"));
	}

	/**
	 * Construct a {@link MessageFuture} that autocompletes if the given timeout
	 * expires.
	 *
	 * @param timeout The timeout value
	 * @param unit    The timeout unit
	 */
	public MessageFuture(long timeout, TimeUnit unit) {
		this(ThreadStore.get("net.message.incoming"), timeout, unit);
	}

	/**
	 * Construct a {@link MessageFuture} that autocompletes if the given timeout
	 * expires.
	 *
	 * @param timeout The timeout value
	 * @param unit    The timeout unit
	 */
	public MessageFuture(EventExecutor executor, long timeout, TimeUnit unit) {
		super(executor);
		var timer = executor.submit(() -> {
			try {
				await(timeout, unit);
			} catch (InterruptedException e) {
				// Ignore
			} finally {
				cancel(true);
			}
		});

		// Kill the timer when the message is received
		addListener(message -> {
			timer.cancel(true);
		});
	}

	/**
	 * Handle the message response.
	 *
	 * @param <E>     The expected response type
	 * @param type    The expected response type
	 * @param handler The message handler
	 * @return {@code this}
	 */
	public <T> MessageFuture handle(Class<T> type, Consumer<T> handler) {

		if (!isDone()) {
			addListener(message -> {
				handler.accept(S7SMsg.of(getNow()).unpack(type));
			});
		}
		return this;
	}

	/**
	 * Convert this {@link MessageFuture} into a new {@link CompletionStage}.
	 *
	 * @param <E>  The expected response type
	 * @param type The expected response type
	 * @return An asynchronous {@link CompletionStage}
	 */
	public <T> CompletionStage<T> toCompletionStage(Class<T> type) {
		var stage = new CompletableFuture<T>();

		addListener(future -> {
			if (future.isSuccess()) {
				stage.complete(S7SMsg.of(getNow()).unpack(type));
			} else {
				stage.completeExceptionally(future.cause());
			}
		});

		return stage;
	}
}
