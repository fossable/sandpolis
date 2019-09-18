/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.core.net.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

/**
 * A Protobuf frame encoder that also accepts messages from other pipelines.
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
			throw new IllegalStateException();

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
