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
package com.sandpolis.core.net.cvid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Metatypes.InstanceFlavor;
import com.sandpolis.core.instance.Metatypes.InstanceType;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.channel.ChannelConstant;
import com.sandpolis.core.net.msg.MsgCvid.RQ_Cvid;
import com.sandpolis.core.net.msg.MsgCvid.RS_Cvid;
import com.sandpolis.core.net.util.CvidUtil;
import com.sandpolis.core.net.util.MsgUtil;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * This handler manages the CVID handshake for the responding instance. Usually
 * the responding instance will be the server.
 *
 * @see CvidRequestHandler
 *
 * @author cilki
 * @since 5.0.0
 */
public class CvidResponseHandler extends AbstractCvidHandler {

	private static final Logger log = LoggerFactory.getLogger(CvidResponseHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MSG msg) throws Exception {
		Channel ch = ctx.channel();
		var sock = ch.attr(ChannelConstant.SOCK).get();

		// Autoremove the handler
		ch.pipeline().remove(this);

		RQ_Cvid rq = MsgUtil.unpack(msg, RQ_Cvid.class);
		if (rq == null || rq.getUuid().isEmpty() || rq.getInstance() == InstanceType.UNRECOGNIZED
				|| rq.getInstance() == InstanceType.SERVER || rq.getInstanceFlavor() == InstanceFlavor.UNRECOGNIZED) {
			log.debug("Received invalid CVID request on channel: {}", ch.id());
			super.userEventTriggered(ctx, new CvidHandshakeCompletionEvent());
		} else {
			int cvid = CvidUtil.cvid(rq.getInstance(), rq.getInstanceFlavor());

			ch.writeAndFlush(MsgUtil
					.rs(msg, RS_Cvid.newBuilder().setServerCvid(Core.cvid()).setServerUuid(Core.UUID).setCvid(cvid))
					.build());

			sock.remoteInstance().set(rq.getInstance());
			sock.remoteCvid().set(cvid);
			sock.remoteUuid().set(rq.getUuid());
			super.userEventTriggered(ctx, new CvidHandshakeCompletionEvent(Core.cvid(), cvid));
		}
	}
}
