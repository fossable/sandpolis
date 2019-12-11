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
package com.sandpolis.core.stream.store;

import static com.sandpolis.core.stream.store.StreamStore.StreamStore;

import java.util.List;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;

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

	@Override
	public void onError(Throwable throwable) {
		StreamStore.stop(id);
		throwable.printStackTrace();
	}

	@Override
	public void onComplete() {
		StreamStore.stop(id);
	}

	public void close() {
		subscription.cancel();
		subscription = null;
	}
}
