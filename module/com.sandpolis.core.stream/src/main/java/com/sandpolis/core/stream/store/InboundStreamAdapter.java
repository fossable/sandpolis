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
