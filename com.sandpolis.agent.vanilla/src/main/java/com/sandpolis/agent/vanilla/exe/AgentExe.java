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
package com.sandpolis.agent.vanilla.exe;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLiteOrBuilder;
import com.sandpolis.core.serveragent.msg.MsgClient.RQ_AgentMetadata;
import com.sandpolis.core.serveragent.msg.MsgClient.RS_AgentMetadata;
import com.sandpolis.core.foundation.util.SystemUtil;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.net.exelet.Exelet;

public final class AgentExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(AgentExe.class);

	@Handler(auth = true)
	public static MessageLiteOrBuilder rq_agent_metadata(RQ_AgentMetadata rq) throws Exception {
		log.trace("rq_client_metadata");

		return RS_AgentMetadata.newBuilder()
				// Network hostname
				.setHostname(InetAddress.getLocalHost().getHostName())
				// OS Family
				.setOs(SystemUtil.OS_TYPE)
				// Base directory location
				.setInstallDirectory(Environment.JAR.path().getParent().toString());
	}

	private AgentExe() {
	}
}
