//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.stream;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.SubmissionPublisher;

import com.google.protobuf.MessageLite;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.channel.HandlerKey;
import com.sandpolis.core.net.connection.Connection;
import com.sandpolis.core.net.util.MsgUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;

public class InboundStreamAdapter<E extends MessageLite> extends SubmissionPublisher<E>
		implements StreamEndpoint, ChannelInboundHandler {

	private int id;
	private Connection sock;
	private Class<E> eventType;

	public InboundStreamAdapter(int streamID, Connection sock, Class<E> eventType) {
		this.id = streamID;
		this.sock = checkNotNull(sock);
		this.eventType = eventType;

		sock.engage(HandlerKey.STREAM, this);
	}

	public Connection getSock() {
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
			submit(MsgUtil.unpack(m, eventType));
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
