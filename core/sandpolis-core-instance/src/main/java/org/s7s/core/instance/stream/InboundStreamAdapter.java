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

import java.util.concurrent.SubmissionPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;
import org.s7s.core.protocol.Message.MSG;
import org.s7s.core.instance.channel.HandlerKey;
import org.s7s.core.instance.connection.Connection;
import org.s7s.core.protocol.Stream.RQ_StopStream;
import org.s7s.core.instance.stream.StreamEndpoint.StreamPublisher;
import org.s7s.core.instance.util.S7SMsg;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;

public class InboundStreamAdapter<E extends MessageLite> extends SubmissionPublisher<E>
		implements StreamPublisher<E>, ChannelInboundHandler {

	private static final Logger log = LoggerFactory.getLogger(InboundStreamAdapter.class);

	private final int id;
	private final Connection sock;
	private final Class<E> eventType;
	private final int eventPayloadId;

	public InboundStreamAdapter(int streamID, Connection sock, Class<E> eventType) {
		this.id = streamID;
		this.sock = checkNotNull(sock);
		this.eventType = eventType;
		this.eventPayloadId = S7SMsg.getPayloadType(eventType);

		log.debug("Engaging stream: {}", id);
		sock.engage(HandlerKey.STREAM, this);
	}

	public Connection getSock() {
		return sock;
	}

	@Override
	public void close() {
		super.close();

		log.debug("Disengaging stream: {}", id);
		sock.disengage(this);
	}

	@Override
	public void closeExceptionally(Throwable error) {
		super.closeExceptionally(error);

		log.trace("Closing due to exception", error);
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
			if (m.getPayloadType() == eventPayloadId) {
				submit(S7SMsg.of(m).unpack(eventType));
			} else if (m.getPayloadType() == S7SMsg.getPayloadType(RQ_StopStream.class)) {
				close();
			} else {
				log.debug("Dropping unknown stream message");
			}

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
