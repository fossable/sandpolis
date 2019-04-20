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
package com.sandpolis.core.net.handler;

import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.Sock.CertificateState;
import com.sandpolis.core.net.Sock.ConnectionState;
import com.sandpolis.core.net.init.ChannelConstant;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;

/**
 * A channel-safe handler that performs logistical functions for a {@link Sock}.
 * Messages and events are not modified by this handler.
 * 
 * @author cilki
 * @since 5.0.0
 */
@Sharable
public class EventHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		var attribute = ctx.channel().attr(ChannelConstant.CONNECTION_STATE);
		synchronized (attribute) {
			attribute.set(ConnectionState.CONNECTED);
		}

		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		var attribute = ctx.channel().attr(ChannelConstant.SOCK);
		if (attribute.get() != null)
			attribute.get().changeState(ConnectionState.NOT_CONNECTED);
		else
			; // The channel was closed before a Sock could be assigned

		ctx.close();
		super.channelInactive(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// TODO Auto-generated method stub
		super.exceptionCaught(ctx, cause);
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof SslHandshakeCompletionEvent) {
			SslHandshakeCompletionEvent event = (SslHandshakeCompletionEvent) evt;

			if (event.isSuccess()) {
				ctx.channel().attr(ChannelConstant.CERTIFICATE_STATE).set(CertificateState.VALID);
			} else {
				Boolean strict = ctx.channel().attr(ChannelConstant.STRICT_CERTS).get();
				if (strict == null || strict) {
					ctx.channel().attr(ChannelConstant.CERTIFICATE_STATE).set(CertificateState.REFUSED);
					ctx.close();
				} else {
					ctx.channel().attr(ChannelConstant.CERTIFICATE_STATE).set(CertificateState.INVALID);
				}
			}
		}

		super.userEventTriggered(ctx, evt);
	}
}
