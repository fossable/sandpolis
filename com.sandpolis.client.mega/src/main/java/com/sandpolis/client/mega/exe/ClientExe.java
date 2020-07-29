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
package com.sandpolis.client.mega.exe;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.cs.msg.MsgClient.RQ_ClientMetadata;
import com.sandpolis.core.cs.msg.MsgClient.RS_ClientMetadata;
import com.sandpolis.core.foundation.util.SystemUtil;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.net.exelet.Exelet;

public final class ClientExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(ClientExe.class);

	@Handler(auth = true)
	public static MessageOrBuilder rq_client_metadata(RQ_ClientMetadata rq) throws Exception {
		log.trace("rq_client_metadata");

		return RS_ClientMetadata.newBuilder()
				// Network hostname
				.setHostname(InetAddress.getLocalHost().getHostName())
				// OS Family
				.setOs(SystemUtil.OS_TYPE)
				// Base directory location
				.setInstallDirectory(Environment.JAR.path().getParent().toString());
	}

	private ClientExe() {
	}
}
