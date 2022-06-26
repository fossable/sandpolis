//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

/**
 * {@link ShortcutFrameEncoder} is a protobuf frame encoder that also accepts
 * messages from other pipelines.
 */
public class ShortcutFrameEncoder extends ProtobufVarint32LengthFieldPrepender {

	private ChannelHandlerContext context;

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		super.handlerAdded(ctx);
		this.context = ctx;
	}

	public void shortcut(ByteBuf msg) throws Exception {
		if (context == null)
			throw new IllegalStateException("The handler has not been added to a pipeline");

		try {
			ByteBuf out = Unpooled.buffer(msg.readableBytes() + computeRawVarint32Size(msg.readableBytes()));
			encode(context, msg, out);
			context.writeAndFlush(out);
		} finally {
			msg.release();
		}
	}

	private int computeRawVarint32Size(final int value) {
		if ((value & (0xffffffff << 7)) == 0)
			return 1;
		if ((value & (0xffffffff << 14)) == 0)
			return 2;
		if ((value & (0xffffffff << 21)) == 0)
			return 3;
		if ((value & (0xffffffff << 28)) == 0)
			return 4;

		return 5;
	}
}
