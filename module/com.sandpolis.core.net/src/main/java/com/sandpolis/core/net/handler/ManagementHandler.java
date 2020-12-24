//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.handler;

import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.net.channel.ChannelConstant;
import com.sandpolis.core.net.connection.Connection;
import com.sandpolis.core.net.connection.ConnectionEvents.SockEstablishedEvent;
import com.sandpolis.core.net.connection.ConnectionEvents.SockLostEvent;
import com.sandpolis.core.net.cvid.AbstractCvidHandler.CvidHandshakeCompletionEvent;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.util.ReferenceCountUtil;

/**
 * A channel-safe handler that performs logistical functions for a
 * {@link Connection}. Messages and events are consumed by this handler.<br>
 * <br>
 * Note: This handler should always be last in the pipeline.
 *
 * @author cilki
 * @since 5.0.0
 */
@Sharable
public class ManagementHandler extends ChannelInboundHandlerAdapter {

	private static final Logger log = LoggerFactory.getLogger(ManagementHandler.class);

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		var connection = ctx.channel().attr(ChannelConstant.SOCK).get();
		ConnectionStore.removeValue(connection);
		ConnectionStore.postAsync(SockLostEvent::new, connection);
		ctx.close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		try {
			log.debug("An exception occurred in the pipeline", cause);
		} finally {
			ReferenceCountUtil.release(cause);
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		try {
			if (evt instanceof SslHandshakeCompletionEvent) {
				SslHandshakeCompletionEvent event = (SslHandshakeCompletionEvent) evt;

				ctx.channel().attr(ChannelConstant.CERTIFICATE_STATE).set(event.isSuccess());
			} else if (evt instanceof CvidHandshakeCompletionEvent) {
				CvidHandshakeCompletionEvent event = (CvidHandshakeCompletionEvent) evt;

				if (event.success) {
					ctx.channel().attr(ChannelConstant.HANDSHAKE_FUTURE).get().setSuccess(null);
					ConnectionStore.postAsync(SockEstablishedEvent::new,
							ctx.channel().attr(ChannelConstant.SOCK).get());
				} else {
					ctx.channel().attr(ChannelConstant.HANDSHAKE_FUTURE).get().setFailure(null);
				}
			}
		} finally {
			ReferenceCountUtil.release(evt);
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		// The message reached the end of the pipeline
		try {
			log.warn("Dropped incoming message: {}", msg.toString());
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}
}
