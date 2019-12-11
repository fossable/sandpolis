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

import static com.sandpolis.core.instance.util.ProtoUtil.begin;
import static com.sandpolis.core.instance.util.ProtoUtil.success;
import static com.sandpolis.core.stream.store.StreamStore.StreamStore;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.util.PlatformUtil;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.exelet.ExeletContext;
import com.sandpolis.core.profile.AttributeStreamSource;
import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.proto.net.MsgAttribute.EV_AttributeStream;
import com.sandpolis.core.proto.net.MsgAttribute.RQ_AttributeQuery;
import com.sandpolis.core.proto.net.MsgAttribute.RQ_AttributeStream;
import com.sandpolis.core.proto.net.MsgAttribute.RS_AttributeQuery;
import com.sandpolis.core.proto.net.MsgClient.RQ_ClientMetadata;
import com.sandpolis.core.proto.net.MsgClient.RS_ClientMetadata;
import com.sandpolis.core.stream.store.OutboundStreamAdapter;

public final class ClientExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(ClientExe.class);

	@Auth
	@Handler(tag = MSG.RQ_CLIENT_METADATA_FIELD_NUMBER)
	public static MessageOrBuilder rq_client_metadata(RQ_ClientMetadata rq) throws Exception {
		log.trace("rq_client_metadata");

		return RS_ClientMetadata.newBuilder()
				// Network hostname
				.setHostname(InetAddress.getLocalHost().getHostName())
				// OS Family
				.setOs(PlatformUtil.OS_TYPE)
				// Base directory location
				.setInstallDirectory(Environment.JAR.path().getParent().toString());
	}

	@Auth
	@Handler(tag = MSG.RQ_ATTRIBUTE_QUERY_FIELD_NUMBER)
	public static MessageOrBuilder rq_attribute_query(RQ_AttributeQuery rq) throws Exception {
		log.trace("rq_attribute_query");

		return RS_AttributeQuery.newBuilder();
	}

	@Auth
	@Handler(tag = MSG.RQ_ATTRIBUTE_STREAM_FIELD_NUMBER)
	public static MessageOrBuilder rq_attribute_stream(ExeletContext context, RQ_AttributeStream rq) throws Exception {
		log.trace("rq_attribute_stream");
		var outcome = begin();

		var source = new AttributeStreamSource(rq.getPathList(), rq.getUpdatePeriod());
		var outbound = new OutboundStreamAdapter<EV_AttributeStream>(rq.getId(), context.connector);
		StreamStore.add(source, outbound);

		return success(outcome);
	}

	private ClientExe() {
	}
}
