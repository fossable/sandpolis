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
package com.sandpolis.core.net.exelet;

import static com.sandpolis.core.net.exelet.ExeletStore.ExeletStore;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.connection.Connection;
import com.sandpolis.core.net.cvid.AbstractCvidHandler.CvidHandshakeCompletionEvent;
import com.sandpolis.core.net.util.CvidUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * A handler that distributes incoming messages to the appropriate
 * {@link Exelet} handler.
 *
 * <p>
 * This handler maintains a separate dispatch vector for each loaded plugin.
 */
public final class ExeletHandler extends SimpleChannelInboundHandler<MSG> {

	private static final Logger log = LoggerFactory.getLogger(ExeletHandler.class);

	final Connection sock;

	Map<String, ExeletMethod> handlers;

	public ExeletHandler(Connection sock) {
		this.sock = sock;
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof CvidHandshakeCompletionEvent) {
			CvidHandshakeCompletionEvent event = (CvidHandshakeCompletionEvent) evt;

			if (event.success) {
				switch (CvidUtil.extractInstance(event.remote)) {
				case AGENT:
					handlers = ExeletStore.agent;
					break;
				case SERVER:
					handlers = ExeletStore.server;
					break;
				case VIEWER:
					handlers = ExeletStore.viewer;
					break;
				default:
					throw new RuntimeException(
							"Cannot create ExeletHandler with remote instance: " + sock.getRemoteInstance().toString());
				}
			}
		}

		ctx.fireUserEventTriggered(evt);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MSG msg) throws Exception {

		var handler = handlers.get(msg.getPayload().getTypeUrl());
		if (handler != null) {
			log.debug("Handling message with exelet: {}", msg.getPayload().getTypeUrl());
			handler.accept(new ExeletContext(sock, msg));
		} else {
			// There's no valid handler
			ctx.fireChannelRead(msg);
		}
	}
}
