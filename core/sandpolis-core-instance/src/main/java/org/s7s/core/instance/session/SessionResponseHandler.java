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
import org.s7s.core.instance.util.S7SSessionID;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * This handler manages the SID handshake for the responding instance. Usually
 * the responding instance will be the server.
 *
 * @see SessionRequestHandler
 *
 * @author cilki
 * @since 5.0.0
 */
public class SessionResponseHandler extends AbstractSessionHandler {

	private static final Logger log = LoggerFactory.getLogger(SessionResponseHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MSG msg) throws Exception {
		Channel ch = ctx.channel();
		var sock = ch.attr(ChannelConstant.SOCK).get();

		// Autoremove the handler
		ch.pipeline().remove(this);

		RQ_Session rq = S7SMsg.of(msg).unpack(RQ_Session.class);
		if (rq == null || rq.getInstanceUuid().isEmpty() || rq.getInstanceType() == InstanceType.UNRECOGNIZED
				|| rq.getInstanceType() == InstanceType.SERVER
				|| rq.getInstanceFlavor() == InstanceFlavor.UNRECOGNIZED) {
			log.debug("Received invalid session request on channel: {}", ch.id());
			super.userEventTriggered(ctx, new SessionHandshakeCompletionEvent());
		} else {
			int sid = S7SSessionID.of(rq.getInstanceType(), rq.getInstanceFlavor()).sid();

			ch.writeAndFlush(S7SMsg.rs(msg).pack(RS_Session.newBuilder().setServerSid(NetworkStore.sid())
					.setServerUuid(Entrypoint.data().uuid()).setInstanceSid(sid)).build());

			sock.set(ConnectionOid.REMOTE_INSTANCE, rq.getInstanceType());
			sock.set(ConnectionOid.REMOTE_SID, sid);
			sock.set(ConnectionOid.REMOTE_UUID, rq.getInstanceUuid());
			super.userEventTriggered(ctx, new SessionHandshakeCompletionEvent(NetworkStore.sid(), sid));
		}
	}
}
