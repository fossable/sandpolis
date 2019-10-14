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
package com.sandpolis.core.net.handler.cvid;

import static com.sandpolis.core.net.store.network.NetworkStore.NetworkStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Core;
import com.sandpolis.core.net.ChannelConstant;
import com.sandpolis.core.net.store.network.NetworkStoreEvents.CvidChangedEvent;
import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.proto.net.MsgCvid.RQ_Cvid;
import com.sandpolis.core.proto.net.MsgCvid.RS_Cvid;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * This handler manages the CVID handshake for the requesting instance. Usually
 * the requesting instance will be the client or viewer.
 *
 * @see CvidResponseHandler
 *
 * @author cilki
 * @since 5.0.0
 */
public class CvidRequestHandler extends AbstractCvidHandler {

	private static final Logger log = LoggerFactory.getLogger(CvidRequestHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MSG msg) throws Exception {
		Channel ch = ctx.channel();

		// Autoremove the handler
		ch.pipeline().remove(this);

		RS_Cvid rs = msg.getRsCvid();
		if (rs != null && !rs.getServerUuid().isEmpty()) {

			Core.setCvid(rs.getCvid());
			NetworkStore.post(CvidChangedEvent::new, Core.cvid());
			ch.attr(ChannelConstant.CVID).set(rs.getServerCvid());
			ch.attr(ChannelConstant.UUID).set(rs.getServerUuid());

			super.userEventTriggered(ctx, new CvidHandshakeCompletionEvent(rs.getCvid()));
			log.debug("CVID handshake succeeded ({})", rs.getCvid());
		} else {
			super.userEventTriggered(ctx, new CvidHandshakeCompletionEvent());
			log.debug("CVID handshake failed");
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		handshake(ctx.channel(), Core.INSTANCE, Core.FLAVOR, Core.UUID);
		super.channelActive(ctx);
	}

	/**
	 * Begin the CVID handshake phase.
	 *
	 * @param channel  The channel
	 * @param instance The instance type
	 * @param flavor   The instance flavor
	 * @param uuid     The instance's UUID
	 */
	void handshake(Channel channel, Instance instance, InstanceFlavor flavor, String uuid) {
		log.debug("Initiating CVID handshake");
		channel.writeAndFlush(MSG.newBuilder()
				.setRqCvid(RQ_Cvid.newBuilder().setInstance(instance).setInstanceFlavor(flavor).setUuid(uuid)).build());
	}

}
