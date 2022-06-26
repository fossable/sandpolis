//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.handler;

import static org.s7s.core.instance.connection.ConnectionStore.ConnectionStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.instance.channel.ChannelConstant;
import org.s7s.core.instance.connection.Connection;
import org.s7s.core.instance.connection.ConnectionStore.SockEstablishedEvent;
import org.s7s.core.instance.connection.ConnectionStore.SockLostEvent;
import org.s7s.core.instance.session.AbstractSessionHandler.SessionHandshakeCompletionEvent;

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
		log.debug("Channel now active: {}", ctx.channel().id());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.debug("Channel now inactive: {}", ctx.channel().id());

		var connection = ctx.channel().attr(ChannelConstant.SOCK).get();
		var handshake_future = ctx.channel().attr(ChannelConstant.HANDSHAKE_FUTURE).get();

		if (!handshake_future.isDone()) {
			handshake_future.cancel(true);
		}

		ConnectionStore.removeValue(connection);
		ConnectionStore.postAsync(new SockLostEvent(connection));
		ctx.close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

		log.debug("An exception occurred in the pipeline", cause);

		var connection = ctx.channel().attr(ChannelConstant.SOCK).get();
		var handshake_future = ctx.channel().attr(ChannelConstant.HANDSHAKE_FUTURE).get();

		if (!handshake_future.isDone()) {
			handshake_future.cancel(true);
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		try {
			if (evt instanceof SslHandshakeCompletionEvent event) {

				ctx.channel().attr(ChannelConstant.CERTIFICATE_STATE).set(event.isSuccess());
			} else if (evt instanceof SessionHandshakeCompletionEvent event) {

				var connection = ctx.channel().attr(ChannelConstant.SOCK).get();
				var handshake_future = ctx.channel().attr(ChannelConstant.HANDSHAKE_FUTURE).get();

				if (event.success) {
					handshake_future.setSuccess(null);
					ConnectionStore.postAsync(new SockEstablishedEvent(connection));
				} else {
					ConnectionStore.removeValue(connection);
					handshake_future.cancel(true);
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
