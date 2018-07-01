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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Core;
import com.sandpolis.core.net.exception.MessageFlowException;
import com.sandpolis.core.net.init.ChannelConstant;
import com.sandpolis.core.proto.net.MCCvid.RQ_Cvid;
import com.sandpolis.core.proto.net.MCCvid.RS_Cvid;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.util.Platform.Instance;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * This handler manages the CVID handshake for the requesting instance. Usually
 * the requesting instance will be the client or viewer.
 * 
 * @see CvidResponseHandler
 * 
 * @author cilki
 * @since 5.0.0
 */
@Sharable
public class CvidRequestHandler extends SimpleChannelInboundHandler<Message> {

	private static final Logger log = LoggerFactory.getLogger(CvidRequestHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
		Channel ch = ctx.channel();

		// Autoremove the handler
		ch.pipeline().remove(this);

		RS_Cvid rs = msg.getRsCvid();
		if (rs != null && !rs.getServerUuid().isEmpty()) {

			Core.setCvid(rs.getCvid());
			ch.attr(ChannelConstant.CVID).set(rs.getServerCvid());
			ch.attr(ChannelConstant.UUID).set(rs.getServerUuid());
			ch.attr(ChannelConstant.HANDLER_CVID).get().setSuccess(rs.getCvid());
		} else {
			ch.attr(ChannelConstant.HANDLER_CVID).get().setFailure(new MessageFlowException(RS_Cvid.class, msg));
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		handshake(ctx.channel(), Core.INSTANCE, Core.uuid());
		super.channelActive(ctx);
	}

	/**
	 * Begin the CVID handshake phase.
	 * 
	 * @param channel
	 *            The channel
	 * @param instance
	 *            The instance type
	 * @param uuid
	 *            The instance's UUID
	 */
	public void handshake(Channel channel, Instance instance, String uuid) {
		log.debug("Initiating CVID handshake");
		channel.writeAndFlush(
				Message.newBuilder().setRqCvid(RQ_Cvid.newBuilder().setInstance(instance).setUuid(uuid)).build());
	}

}
