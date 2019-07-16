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
package com.sandpolis.core.stream.store;

import java.util.List;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;

import com.sandpolis.core.proto.net.MCStream.EV_StreamData;
import com.sandpolis.core.util.ProtoUtil;

public abstract class StreamSink<E> implements Subscriber<E>, StreamEndpoint {

	private int id;
	private List<Consumer<E>> handlers;
	private Subscription subscription;

	@Override
	public int getStreamID() {
		return id;
	}

	public void addHandler(Consumer<E> handler) {
		handlers.add(handler);
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		this.subscription = subscription;
		this.subscription.request(Long.MAX_VALUE);
	}

	@Override
	public void onNext(E item) {
		handlers.forEach(handler -> handler.accept(item));
	}

	@SuppressWarnings("unchecked")
	public void onNext(EV_StreamData data) {
		onNext((E) ProtoUtil.getPayload(data));
	}

	@Override
	public void onError(Throwable throwable) {
		StreamStore.sink.remove(this);
		throwable.printStackTrace();
	}

	@Override
	public void onComplete() {
		StreamStore.sink.remove(this);
	}

	public void close() {
		subscription.cancel();
		subscription = null;
	}
}