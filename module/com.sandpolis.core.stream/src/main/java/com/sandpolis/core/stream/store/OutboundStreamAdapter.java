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

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.proto.net.MCStream.EV_StreamData;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.util.ProtoUtil;

public class OutboundStreamAdapter<E extends MessageOrBuilder> implements Subscriber<E>, StreamEndpoint {

	private int id;
	private Sock sock;
	private Subscription subscription;

	@Override
	public int getStreamID() {
		return id;
	}

	public Sock getSock() {
		return sock;
	}

	public OutboundStreamAdapter(int streamID, Sock sock) {
		this.id = streamID;
		this.sock = checkNotNull(sock);
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		this.subscription = subscription;
		this.subscription.request(Long.MAX_VALUE);
	}

	@Override
	public void onError(Throwable throwable) {
		StreamStore.outbound.remove(this);
		throwable.printStackTrace();
	}

	@Override
	public void onComplete() {
		StreamStore.outbound.remove(this);
	}

	@Override
	public void onNext(E item) {
		sock.send(
				Message.newBuilder().setEvStreamData(ProtoUtil.setPayload(EV_StreamData.newBuilder().setId(id), item)));
	}
}
