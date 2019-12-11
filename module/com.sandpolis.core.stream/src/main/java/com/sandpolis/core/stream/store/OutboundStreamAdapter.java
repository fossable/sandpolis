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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sandpolis.core.stream.store.StreamStore.StreamStore;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Function;

import com.google.protobuf.Any;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.util.ProtoUtil;
import com.sandpolis.core.net.sock.Sock;
import com.sandpolis.core.proto.net.Message.MSG;

public class OutboundStreamAdapter<E extends MessageOrBuilder> implements Subscriber<E>, StreamEndpoint {

	private int id;
	private Sock sock;
	private Subscription subscription;
	private Function<E, Any> pluginPacker;
	private int cvid;

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

	public OutboundStreamAdapter(int streamID, Sock sock, int cvid, Function<E, Any> packer) {
		this.id = streamID;
		this.sock = checkNotNull(sock);
		this.pluginPacker = packer;
		this.cvid = cvid;
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		this.subscription = subscription;
		this.subscription.request(Long.MAX_VALUE);
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

	@Override
	public void onNext(E item) {
		if (pluginPacker == null)
			sock.send(ProtoUtil.setPayload(MSG.newBuilder().setId(id), item));
		else
			sock.send(MSG.newBuilder().setTo(cvid).setFrom(Core.cvid()).setId(id).setPlugin(pluginPacker.apply(item)));
	}
}
