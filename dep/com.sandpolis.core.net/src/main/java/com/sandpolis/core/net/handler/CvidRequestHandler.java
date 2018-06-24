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
import com.sandpolis.core.instance.Instance;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.exception.MessageFlowException;
import com.sandpolis.core.proto.net.MCCvid.RQ_Cvid;
import com.sandpolis.core.proto.net.MCCvid.RS_Cvid;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.util.IDUtil;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * This handler manages the CVID handshake from the requesting instance. Usually
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

		autoremove(ch);
		RS_Cvid rs = msg.getRsCvid();
		if (rs != null && !rs.getServerUuid().isEmpty()) {

			Core.setCvid(rs.getCvid());
			ch.attr(Sock.CVID_KEY).set(rs.getServerCvid());
			ch.attr(Sock.UUID_KEY).set(rs.getServerUuid());
			ch.attr(Sock.CVID_HANDLER_KEY).get().complete(null);
		} else {
			ch.attr(Sock.CVID_HANDLER_KEY).get().completeExceptionally(new MessageFlowException(RS_Cvid.class, msg));
		}
	}

	/**
	 * Begin the CVID handshake.
	 * 
	 * @param channel
	 *            The channel
	 * @param instance
	 *            The requesting instance
	 * @param uuid
	 *            The requesting instance's UUID
	 */
	public void initiateHandshake(Channel channel, Instance instance, String uuid) {
		log.debug("Initiating CVID handshake");
		channel.writeAndFlush(Message.newBuilder()
				.setRqCvid(RQ_Cvid.newBuilder().setIid(IDUtil.CVID.getIID(instance)).setUuid(uuid)).build());
	}

	/**
	 * Remove the handler from the pipeline once the negotation is over.
	 */
	private void autoremove(Channel channel) {
		channel.pipeline().remove(this);
	}

	@Override
	public boolean isSharable() {
		return true;
	}

}
