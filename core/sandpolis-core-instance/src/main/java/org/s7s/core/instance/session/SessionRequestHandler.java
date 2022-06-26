//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.session;

import static org.s7s.core.instance.network.NetworkStore.NetworkStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.instance.Entrypoint;
import org.s7s.core.foundation.Instance.InstanceFlavor;
import org.s7s.core.foundation.Instance.InstanceType;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ConnectionOid;
import org.s7s.core.protocol.Message.MSG;
import org.s7s.core.protocol.Session.RQ_Session;
import org.s7s.core.protocol.Session.RS_Session;
import org.s7s.core.instance.channel.ChannelConstant;
import org.s7s.core.instance.util.S7SMsg;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * This handler manages the SID handshake for the requesting instance. Usually
 * the requesting instance will be the agent or client.
 *
 * @see SessionResponseHandler
 *
 * @since 5.0.0
 */
public class SessionRequestHandler extends AbstractSessionHandler {

	private static final Logger log = LoggerFactory.getLogger(SessionRequestHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MSG msg) throws Exception {
		var ch = ctx.channel();
		var sock = ch.attr(ChannelConstant.SOCK).get();

		// Autoremove the handler
		ch.pipeline().remove(this);

		var rs = S7SMsg.of(msg).unpack(RS_Session.class);
		if (rs == null) {
			log.debug("Session handshake failed");
			userEventTriggered(ctx, new SessionHandshakeCompletionEvent());
		}

		// Check core components
		// TODO

		// Succeeded
		NetworkStore.setSid(rs.getInstanceSid());
		sock.set(ConnectionOid.REMOTE_SID, rs.getServerSid());
		sock.set(ConnectionOid.REMOTE_UUID, rs.getServerUuid());

		log.debug("Session handshake succeeded ({})", rs.getInstanceSid());
		userEventTriggered(ctx, new SessionHandshakeCompletionEvent(rs.getInstanceSid(), rs.getServerSid()));
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		handshake(ctx.channel(), Entrypoint.data().instance(), Entrypoint.data().flavor(), Entrypoint.data().uuid());
		super.channelActive(ctx);
	}

	/**
	 * Begin the SID handshake phase.
	 *
	 * @param channel  The channel
	 * @param instance The instance type
	 * @param flavor   The instance flavor
	 * @param uuid     The instance's UUID
	 */
	void handshake(Channel channel, InstanceType instance, InstanceFlavor flavor, String uuid) {
		log.debug("Initiating session handshake");
		channel.writeAndFlush(S7SMsg.rq()
				.pack(RQ_Session.newBuilder().setInstanceType(instance).setInstanceFlavor(flavor).setInstanceUuid(uuid))
				.build());
	}
}
