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
package com.sandpolis.core.server.banner;

import static com.sandpolis.core.server.banner.BannerStore.BannerStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLiteOrBuilder;
import com.sandpolis.core.instance.msg.MsgPing.RQ_Ping;
import com.sandpolis.core.instance.msg.MsgPing.RS_Ping;
import com.sandpolis.core.net.exelet.Exelet;
import com.sandpolis.core.clientserver.msg.MsgServer.RQ_ServerBanner;

/**
 * {@link BannerExe} contains message handlers for server banner requests.
 *
 * @since 4.0.0
 */
public final class BannerExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(BannerExe.class);

	@Handler(auth = false)
	public static MessageLiteOrBuilder rq_server_banner(RQ_ServerBanner rq) {
		return BannerStore.getBanner();
	}

	@Handler(auth = false)
	public static MessageLiteOrBuilder rq_ping(RQ_Ping rq) {
		return RS_Ping.newBuilder();
	}

	private BannerExe() {
	}
}
