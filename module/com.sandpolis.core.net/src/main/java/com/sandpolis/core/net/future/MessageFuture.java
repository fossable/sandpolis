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

import java.util.concurrent.TimeUnit;

import com.sandpolis.core.net.Message.MSG;

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
}
