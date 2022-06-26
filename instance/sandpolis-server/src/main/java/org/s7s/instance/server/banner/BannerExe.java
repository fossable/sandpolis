//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.banner;

import static org.s7s.core.server.banner.BannerStore.BannerStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLiteOrBuilder;
import org.s7s.core.protocol.Server.RQ_ServerBanner;
import org.s7s.core.protocol.Network.RQ_Ping;
import org.s7s.core.protocol.Network.RS_Ping;
import org.s7s.core.instance.exelet.Exelet;

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
	public static RS_Ping rq_ping(RQ_Ping rq) {
		return RS_Ping.PING_OK;
	}

	private BannerExe() {
	}
}
