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

import java.util.concurrent.TimeUnit;

import com.sandpolis.core.proto.net.Message.MSG;

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
