/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.server.vanilla.exe;

import com.google.protobuf.Message;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.proto.net.MCStream.ProfileStreamData;
import com.sandpolis.core.proto.net.MCStream.RQ_StreamStart;
import com.sandpolis.core.proto.net.MCStream.RQ_StreamStop;
import com.sandpolis.core.proto.net.MCStream.RS_StreamStart;
import com.sandpolis.core.proto.net.MSG;
import com.sandpolis.core.stream.Stream;
import com.sandpolis.core.stream.store.OutboundStreamAdapter;
import com.sandpolis.core.stream.store.StreamStore;
import com.sandpolis.server.vanilla.stream.ProfileStreamSource;

/**
 * Stream message handlers.
 * 
 * @author cilki
 * @since 5.0.2
 */
public class StreamExe extends Exelet {

	public StreamExe(Sock connector) {
		super(connector);
	}

	@Auth
	public Message.Builder rq_stream_start(RQ_StreamStart rq) {
		var outcome = begin();

		Stream stream = new Stream();
		switch (rq.getParam().getTypeCase()) {
		case PROFILE:
			ProfileStreamSource source = new ProfileStreamSource();// TODO get from store
			source.start();
			source.addOutbound(new OutboundStreamAdapter<ProfileStreamData>(stream.getStreamID(), connector));
			break;
		default:
			return failure(outcome);
		}

		return RS_StreamStart.newBuilder().setStreamID(stream.getStreamID());
	}

	@Auth
	public Message.Builder rq_stream_stop(RQ_StreamStop rq) {
		var outcome = begin();

		StreamStore.stop(rq.getStreamID());
		return success(outcome);
	}

	@Auth
	public void ev_stream_data(MSG.Message m) {
		StreamStore.streamData(m.getEvStreamData());
	}

}
