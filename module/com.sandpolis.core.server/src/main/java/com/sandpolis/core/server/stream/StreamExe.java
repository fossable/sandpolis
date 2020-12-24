//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.server.stream;

import static com.sandpolis.core.foundation.util.ProtoUtil.begin;
import static com.sandpolis.core.foundation.util.ProtoUtil.success;
import static com.sandpolis.core.net.stream.StreamStore.StreamStore;

import com.google.protobuf.MessageLiteOrBuilder;
import com.sandpolis.core.net.exelet.Exelet;
import com.sandpolis.core.net.msg.MsgStream.RQ_StreamStop;

/**
 * Stream message handlers.
 *
 * @author cilki
 * @since 5.0.2
 */
public final class StreamExe extends Exelet {

//	@Handler(auth = true)
//	public static MessageOrBuilder rq_profile_stream(ExeletContext context, RQ_ProfileStream rq) {
//		var outcome = begin();
//
//		context.defer(() -> {
//			ProfileStreamSource source = new ProfileStreamSource();// TODO get from store
//			StreamStore.add(source, new OutboundStreamAdapter<EV_ProfileStream>(rq.getId(), context.connector));
//			source.start();
//		});
//
//		return success(outcome);
//	}

	@Handler(auth = true)
	public static MessageLiteOrBuilder rq_stream_stop(RQ_StreamStop rq) {
		var outcome = begin();

		StreamStore.stop(rq.getId());
		return success(outcome);
	}

	private StreamExe() {
	}
}
