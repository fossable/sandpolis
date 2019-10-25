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
package com.sandpolis.core.stream.store;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.SubmissionPublisher;
import java.util.function.Function;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.sock.Sock;
import com.sandpolis.core.proto.net.Message.MSG;

public class InboundStreamAdapter<E extends MessageOrBuilder> extends SubmissionPublisher<E> implements StreamEndpoint {

	private int id;
	private Sock sock;
	private Function<MSG, E> converter;

	@Override
	public int getStreamID() {
		return id;
	}

	public Sock getSock() {
		return sock;
	}

	public InboundStreamAdapter(int streamID, Sock sock, Function<MSG, E> converter) {
		this.id = streamID;
		this.sock = checkNotNull(sock);
		this.converter = converter;
	}

	public int submit(MSG item) {
		return super.submit(converter.apply(item));
	}
}
