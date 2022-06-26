//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.proxy;

import static org.s7s.core.instance.connection.ConnectionStore.ConnectionStore;

import org.s7s.core.instance.state.InstanceOids.ProfileOid.ConnectionOid;
import org.s7s.core.protocol.Message.MSG;
import org.s7s.core.instance.channel.ChannelConstant;
import org.s7s.core.instance.channel.HandlerKey;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.util.ReferenceCountUtil;

/**
 * {@link ProxyHandler} reads the first two fields of an incoming {@link MSG} to
 * determine its destination. If the destination is another instance, the
 * {@link ByteBuf} will be efficiently forwarded without decoding the entire
 * message. Otherwise the {@link ByteBuf} will be decoded and executed for this
 * instance.
 *
 * <p>
 * This handler MUST be placed after a {@link ProtobufVarint32FrameDecoder}!
 * Otherwise a malicious instance could send messages to unauthorized instances
 * by sending two messages in rapid succession. Since this handler only verifies
 * the first few bytes and then routes the entire buffer accordingly, sending
 * one small valid message followed by an invalid message would lead to the
 * invalid message being delivered to the receiver.
 *
 * @since 5.0.0
 */
@Sharable
public class ProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {

	/**
	 * The server's SID which is used in determining when to route messages.
	 */
	private final int sid;

	public ProxyHandler(int sid) {
		this.sid = sid;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		msg.markReaderIndex();

		// Read field: "to"
		if (msg.isReadable() && msg.readByte() == 0x08) {
			int to = readSid(msg);

			// Perform redirection if necessary
			if (to != sid) {

				// Read field: "from"
				if (msg.isReadable() && msg.readByte() == 0x10) {
					int from = readSid(msg);

					// Verify the from field to prevent spoofing
					if (ctx.channel().attr(ChannelConstant.SOCK).get().get(ConnectionOid.REMOTE_SID).asInt() != from) {
						throw new ChannelException("Message 'from' does not match channel's SID");
					}
				} else {
					throw new ChannelException("Message specifies 'to' but not 'from'");
				}

				// Route the message
				var sock = ConnectionStore.getBySid(to);
				if (sock.isPresent()) {
					msg.resetReaderIndex();
					msg.retain();

					// Skip to the middle of the pipeline
					sock.get().getHandler(HandlerKey.FRAME_ENCODER).get().shortcut(msg);
				}

				return;
			}
		}

		msg.resetReaderIndex();
		ReferenceCountUtil.retain(msg);
		ctx.fireChannelRead(msg);
	}

	/**
	 * Read a SID varint from the given {@link ByteBuf}.
	 *
	 * @param buffer The buffer to read (read pointer will be modified)
	 * @return The decoded SID
	 */
	private static int readSid(ByteBuf buffer) {
		byte tmp = buffer.readByte();
		if (tmp >= 0) {
			return tmp;
		} else {
			int result = tmp & 127;
			if ((tmp = buffer.readByte()) >= 0) {
				result |= tmp << 7;
			} else {
				result |= (tmp & 127) << 7;
				if ((tmp = buffer.readByte()) >= 0) {
					result |= tmp << 14;
				} else {
					result |= (tmp & 127) << 14;
					if ((tmp = buffer.readByte()) >= 0) {
						result |= tmp << 21;
					} else {
						result |= (tmp & 127) << 21;
						result |= (tmp = buffer.readByte()) << 28;
						if (tmp < 0) {
							// The SID is negative (negative varints are always 10 bytes)
							throw new CorruptedFrameException("Invalid SID");
						}
					}
				}
			}
			if (result < 0)
				throw new CorruptedFrameException("Invalid SID");

			return result;
		}
	}
}
