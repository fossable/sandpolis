//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.stream;

import static org.s7s.core.instance.stream.StreamStore.StreamStore;

import org.s7s.core.instance.exelet.Exelet;
import org.s7s.core.protocol.Stream.RQ_StopStream;
import org.s7s.core.protocol.Stream.RS_StopStream;

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
	public static RS_StopStream rq_stop_stream(RQ_StopStream rq) {

		StreamStore.stop(rq.getId());
		return RS_StopStream.STOP_STREAM_OK;
	}

	private StreamExe() {
	}
}
