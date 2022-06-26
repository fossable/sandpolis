//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.s7s.core.instance.stream.StreamStore.StreamStore;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLiteOrBuilder;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ConnectionOid;
import org.s7s.core.instance.connection.Connection;
import org.s7s.core.protocol.Stream.RQ_StopStream;
import org.s7s.core.instance.stream.StreamEndpoint.StreamSubscriber;
import org.s7s.core.instance.util.S7SMsg;

public class OutboundStreamAdapter<E extends MessageLiteOrBuilder> implements Subscriber<E>, StreamSubscriber<E> {

	private static final Logger log = LoggerFactory.getLogger(OutboundStreamAdapter.class);

	private final int sid;
	private final int id;
	private final Connection connection;
	private Subscription subscription;

	public OutboundStreamAdapter(int streamID, Connection sock) {
		this.id = streamID;
		this.connection = checkNotNull(sock);
		this.sid = connection.get(ConnectionOid.REMOTE_SID).asInt();
	}

	public OutboundStreamAdapter(int streamID, Connection sock, int sid) {
		this.id = streamID;
		this.connection = checkNotNull(sock);
		this.sid = sid;
	}

	public Connection getSock() {
		return connection;
	}

	@Override
	public int getStreamID() {
		return id;
	}

	@Override
	public void onComplete() {
		log.debug("onComplete");
		StreamStore.stop(id);
	}

	@Override
	public void onError(Throwable throwable) {
		StreamStore.stop(id);
		log.error("Publish or subscription failure", throwable);
	}

	@Override
	public void onNext(E item) {
		connection.send(S7SMsg.ev(id).pack(item).setTo(sid));
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		this.subscription = subscription;
		this.subscription.request(Long.MAX_VALUE);
	}

	@Override
	public void close() {

		// TODO send this message after all previous messages have been processed
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (connection.channel().isActive()) {
			log.debug("Sending stream closed event");
			connection.send(S7SMsg.ev(id).pack(RQ_StopStream.newBuilder()).setTo(sid));
		}
	}
}
