//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.stream;

import static org.s7s.core.instance.stream.StreamStore.StreamStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;

import org.s7s.core.instance.stream.StreamEndpoint.StreamSubscriber;

public abstract class StreamSink<E> implements Subscriber<E>, StreamSubscriber<E> {

	private List<Consumer<E>> handlers;
	private int id;
	private Subscription subscription;

	public StreamSink() {
		this.handlers = new ArrayList<>();
	}

	public void addHandler(Consumer<E> handler) {
		handlers.add(handler);
	}

	@Override
	public void close() {
		subscription.cancel();
		subscription = null;
	}

	@Override
	public int getStreamID() {
		return id;
	}

	@Override
	public void onComplete() {
		StreamStore.stop(id);
	}

	@Override
	public void onError(Throwable throwable) {
		StreamStore.stop(id);
		throwable.printStackTrace();
	}

	@Override
	public void onNext(E item) {
		handlers.forEach(handler -> handler.accept(item));
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		this.subscription = subscription;
		this.subscription.request(Long.MAX_VALUE);
	}
}
