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

import static com.sandpolis.core.instance.util.ProtoUtil.begin;
import static com.sandpolis.core.instance.util.ProtoUtil.success;
import static com.sandpolis.core.net.stream.StreamStore.StreamStore;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.exelet.ExeletContext;
import com.sandpolis.core.net.stream.OutboundStreamAdapter;
import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.proto.net.MsgStream.EV_ProfileStream;
import com.sandpolis.core.proto.net.MsgStream.RQ_ProfileStream;
import com.sandpolis.core.proto.net.MsgStream.RQ_StreamStop;
import com.sandpolis.server.vanilla.stream.ProfileStreamSource;

/**
 * Stream message handlers.
 *
 * @author cilki
 * @since 5.0.2
 */
public final class StreamExe extends Exelet {

	@Auth
	@Handler(tag = MSG.RQ_PROFILE_STREAM_FIELD_NUMBER)
	public static MessageOrBuilder rq_profile_stream(ExeletContext context, RQ_ProfileStream rq) {
		var outcome = begin();
		// TODO use correct stream ID
		// Stream stream = new Stream();

		context.defer(() -> {
			ProfileStreamSource source = new ProfileStreamSource();// TODO get from store
			StreamStore.add(source, new OutboundStreamAdapter<EV_ProfileStream>(rq.getId(), context.connector));
			source.start();
		});

		return success(outcome);
	}

	@Auth
	@Handler(tag = MSG.RQ_STREAM_STOP_FIELD_NUMBER)
	public static MessageOrBuilder rq_stream_stop(RQ_StreamStop rq) {
		var outcome = begin();

		StreamStore.stop(rq.getId());
		return success(outcome);
	}

	private StreamExe() {
	}
}
