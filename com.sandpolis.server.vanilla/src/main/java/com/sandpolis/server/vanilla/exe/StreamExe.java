/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.server.vanilla.exe;

import static com.sandpolis.core.instance.util.ProtoUtil.begin;
import static com.sandpolis.core.instance.util.ProtoUtil.success;
import static com.sandpolis.core.stream.store.StreamStore.StreamStore;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.exelet.ExeletContext;
import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.proto.net.MsgStream.EV_ProfileStream;
import com.sandpolis.core.proto.net.MsgStream.RQ_ProfileStream;
import com.sandpolis.core.proto.net.MsgStream.RQ_StreamStop;
import com.sandpolis.core.stream.store.OutboundStreamAdapter;
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
			source.addOutbound(new OutboundStreamAdapter<EV_ProfileStream>(rq.getId(), context.connector));
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
