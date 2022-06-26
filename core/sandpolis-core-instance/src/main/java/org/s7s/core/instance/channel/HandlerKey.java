//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.channel;

import org.s7s.core.instance.session.AbstractSessionHandler;
import org.s7s.core.instance.exelet.ExeletHandler;
import org.s7s.core.instance.handler.ManagementHandler;
import org.s7s.core.instance.handler.ResponseHandler;
import org.s7s.core.instance.handler.ShortcutFrameEncoder;
import org.s7s.core.instance.stream.InboundStreamAdapter;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

public final class HandlerKey<E extends ChannelHandler> {

	public static final HandlerKey<AbstractSessionHandler> SESSION = new HandlerKey<>("SessionHandler");
	public static final HandlerKey<ExeletHandler> EXELET = new HandlerKey<>("ExeletHandler");
	public static final HandlerKey<ProtobufVarint32FrameDecoder> FRAME_DECODER = new HandlerKey<>("FrameDecoder");
	public static final HandlerKey<ShortcutFrameEncoder> FRAME_ENCODER = new HandlerKey<>("FrameEncoder");
	public static final HandlerKey<LoggingHandler> LOG_DECODED = new HandlerKey<>("DecodedLogHandler");
	public static final HandlerKey<LoggingHandler> LOG_RAW = new HandlerKey<>("RawLogHandler");
	public static final HandlerKey<ManagementHandler> MANAGEMENT = new HandlerKey<>("EventHandler");
	public static final HandlerKey<ProtobufDecoder> PROTO_DECODER = new HandlerKey<>("ProtoDecoder");
	public static final HandlerKey<ProtobufEncoder> PROTO_ENCODER = new HandlerKey<>("ProtoEncoder");
	public static final HandlerKey<ResponseHandler> RESPONSE = new HandlerKey<>("ResponseHandler");
	public static final HandlerKey<InboundStreamAdapter> STREAM = new HandlerKey<>("StreamAdapter");
	public static final HandlerKey<SslHandler> TLS = new HandlerKey<>("TlsHandler");
	public static final HandlerKey<ChannelTrafficShapingHandler> TRAFFIC = new HandlerKey<>("TrafficHandler");

	public final String base;

	public HandlerKey(String base) {
		this.base = base;
	}

	/**
	 * Return the next unused handler name for the given pipeline.
	 *
	 * @param pipeline The target pipeline
	 * @return The handler's name in the target pipeline
	 */
	public String next(ChannelPipeline pipeline) {
		var names = pipeline.names();

		for (int i = 0; i < 8192; i++) {
			String name = base + "#" + i;
			if (!names.contains(name))
				return name;
		}

		// Exceeding the limit is indicative of other problems
		throw new RuntimeException("Too many handlers in pipeline");
	}
}
