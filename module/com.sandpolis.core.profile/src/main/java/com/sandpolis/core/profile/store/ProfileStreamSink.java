package com.sandpolis.core.profile.store;

import com.sandpolis.core.net.stream.StreamSink;
import com.sandpolis.core.proto.net.MsgStream.EV_ProfileStream;

public class ProfileStreamSink extends StreamSink<EV_ProfileStream> {

	public ProfileStreamSink() {
		addHandler(ev -> {
			// TODO
		});
	}
}
