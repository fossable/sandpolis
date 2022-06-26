//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.handler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.s7s.core.protocol.Message.MSG;
import org.s7s.core.instance.message.MessageFuture;

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
