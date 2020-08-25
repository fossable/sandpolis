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
package com.sandpolis.core.net.stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sandpolis.core.net.stream.StreamStore.StreamStore;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.connection.Connection;
import com.sandpolis.core.net.util.MsgUtil;

public class OutboundStreamAdapter<E extends MessageOrBuilder> implements Subscriber<E>, StreamEndpoint {

	private int cvid;
	private int id;
	private Connection sock;
	private Subscription subscription;

	public OutboundStreamAdapter(int streamID, Connection sock) {
		this.id = streamID;
		this.sock = checkNotNull(sock);
	}

	public OutboundStreamAdapter(int streamID, Connection sock, int cvid) {
		this.id = streamID;
		this.sock = checkNotNull(sock);
		this.cvid = cvid;
	}

	public Connection getSock() {
		return sock;
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
		sock.send(MsgUtil.ev(id, item));
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		this.subscription = subscription;
		this.subscription.request(Long.MAX_VALUE);
	}

	public void close() {

	}
}
