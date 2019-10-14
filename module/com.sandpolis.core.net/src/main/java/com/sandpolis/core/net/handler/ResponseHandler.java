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
package com.sandpolis.core.net.handler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.sandpolis.core.net.future.MessageFuture;
import com.sandpolis.core.proto.net.Message.MSG;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author cilki
 * @since 5.1.0
 */
public final class ResponseHandler extends SimpleChannelInboundHandler<MSG> {

	/**
	 * When a response message is desired, a {@link MessageFuture} is placed into
	 * this map. If a message is received which is not associated with any handler
	 * in {@link #handles} and the message's ID is in {@link #responseMap}, the
	 * MessageFuture is removed and notified.
	 */
	private final ConcurrentMap<Integer, MessageFuture> responseMap;

	public ResponseHandler() {
		this.responseMap = new ConcurrentHashMap<>();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MSG msg) throws Exception {
		MessageFuture future = responseMap.remove(msg.getId());
		if (future != null && !future.isCancelled()) {
			// Give the message to a waiting Thread
			future.setSuccess(msg);
			return;
		}

		ctx.fireChannelRead(msg);
	}

	/**
	 * Add a new response callback to the response map unless one is already
	 * present.
	 *
	 * @param id     The message ID
	 * @param future A new {@link MessageFuture}
	 * @return An existing future or the given parameter
	 */
	public MessageFuture putResponseFuture(int id, MessageFuture future) {
		if (!responseMap.containsKey(id))
			responseMap.put(id, future);

		return responseMap.get(id);
	}

	/**
	 * Get the number of futures waiting for a response.
	 *
	 * @return The number of entries in the response map
	 */
	public int getResponseCount() {
		return responseMap.size();
	}
}
