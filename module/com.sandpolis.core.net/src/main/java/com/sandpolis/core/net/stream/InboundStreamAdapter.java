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

import java.util.concurrent.SubmissionPublisher;
import java.util.function.Function;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.HandlerKey;
import com.sandpolis.core.net.sock.Sock;
import com.sandpolis.core.proto.net.Message.MSG;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;

public class InboundStreamAdapter<E extends MessageOrBuilder> extends SubmissionPublisher<E>
		implements StreamEndpoint, ChannelInboundHandler {

	private Function<MSG, E> converter;
	private int id;
	private Sock sock;

	public InboundStreamAdapter(int streamID, Sock sock, Function<MSG, E> converter) {
		this.id = streamID;
		this.sock = checkNotNull(sock);
		this.converter = converter;

		sock.engage(HandlerKey.STREAM, this);
	}

	public Sock getSock() {
		return sock;
	}

	@Override
	public void close() {
		super.close();

		sock.disengage(this);
	}

	@Override
	public void closeExceptionally(Throwable error) {
		super.closeExceptionally(error);

		sock.disengage(this);
	}

	@Override
	public int getStreamID() {
		return id;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		// NO OP
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		// NO OP
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelRegistered();
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelUnregistered();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelActive();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelInactive();
		close();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		MSG m = (MSG) msg;
		if (m.getId() == id) {
			submit(converter.apply(m));
		} else {
			ctx.fireChannelRead(msg);
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelReadComplete();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		ctx.fireUserEventTriggered(evt);
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelWritabilityChanged();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.fireExceptionCaught(cause);
	}
}
