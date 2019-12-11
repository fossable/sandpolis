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
package com.sandpolis.server.vanilla.exe;

import static com.sandpolis.server.vanilla.store.server.ServerStore.ServerStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.proto.net.MsgPing.RQ_Ping;
import com.sandpolis.core.proto.net.MsgPing.RS_Ping;
import com.sandpolis.core.proto.net.MsgServer.RQ_ServerBanner;

/**
 * Message handlers for server requests.
 *
 * @author cilki
 * @since 4.0.0
 */
public final class ServerExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(ServerExe.class);

	@Unauth
	@Handler(tag = MSG.RQ_SERVER_BANNER_FIELD_NUMBER)
	public static MessageOrBuilder rq_server_banner(RQ_ServerBanner rq) {
		return ServerStore.getBanner();
	}

	@Unauth
	@Handler(tag = MSG.RQ_PING_FIELD_NUMBER)
	public static MessageOrBuilder rq_ping(RQ_Ping rq) {
		return RS_Ping.newBuilder();
	}

	private ServerExe() {
	}
}
