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

import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.init.ChannelConstant;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.DefaultPromise;

/**
 * A {@link SockFuture} provides an easy way to obtain a configured {@link Sock}
 * from a {@link ChannelFuture}.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class SockFuture extends DefaultPromise<Sock> {

	/**
	 * Construct a new {@link SockFuture} which will create a new {@link Sock} once
	 * the given connection is established.
	 * 
	 * @param connect A connection future
	 */
	public SockFuture(ChannelFuture connect) {
		super(connect.channel().eventLoop());

		connect.addListener((ChannelFuture future) -> {
			if (!future.isSuccess())
				SockFuture.this.setFailure(future.cause());
			Channel ch = future.channel();

			if (ch.attr(ChannelConstant.FUTURE_CVID).get() != null) {
				// Wait for the CVID handshake to complete
				ch.attr(ChannelConstant.FUTURE_CVID).get().addListener((DefaultPromise<Integer> promise) -> {
					if (!promise.isSuccess()) {
						SockFuture.this.setFailure(promise.cause());
						return;
					}

					complete(ch);
				});
			} else {
				complete(ch);
			}
		});
	}

	/**
	 * Complete the future successfully.
	 * 
	 * @param channel The successful channel
	 */
	private void complete(Channel channel) {
		if (isDone())
			throw new IllegalStateException();

		Sock sock = new Sock(channel);
		channel.attr(ChannelConstant.SOCK).set(sock);

		setSuccess(sock);
	}
}
