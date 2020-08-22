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
package com.sandpolis.core.server.channel;

import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.channel.HandlerKey.CVID;
import static com.sandpolis.core.net.channel.HandlerKey.EXELET;
import static com.sandpolis.core.net.channel.HandlerKey.FRAME_DECODER;
import static com.sandpolis.core.net.channel.HandlerKey.FRAME_ENCODER;
import static com.sandpolis.core.net.channel.HandlerKey.LOG_DECODED;
import static com.sandpolis.core.net.channel.HandlerKey.LOG_RAW;
import static com.sandpolis.core.net.channel.HandlerKey.MANAGEMENT;
import static com.sandpolis.core.net.channel.HandlerKey.PROTO_DECODER;
import static com.sandpolis.core.net.channel.HandlerKey.PROTO_ENCODER;
import static com.sandpolis.core.net.channel.HandlerKey.RESPONSE;
import static com.sandpolis.core.net.channel.HandlerKey.TLS;
import static com.sandpolis.core.net.channel.HandlerKey.TRAFFIC;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;

import java.util.function.Consumer;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.foundation.Config;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.channel.ChannelConfig;
import com.sandpolis.core.net.channel.ChannelConstant;
import com.sandpolis.core.net.channel.HandlerKey;
import com.sandpolis.core.net.connection.Connection;
import com.sandpolis.core.net.cvid.CvidResponseHandler;
import com.sandpolis.core.net.exelet.ExeletHandler;
import com.sandpolis.core.net.handler.ManagementHandler;
import com.sandpolis.core.net.handler.ResponseHandler;
import com.sandpolis.core.server.proxy.ProxyHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

/**
 * {@link ServerChannelInitializer} configures a {@link Channel} for use in a
 * connection to a client or viewer.
 *
 * @since 5.0.0
 */
public class ServerChannelInitializer extends ChannelInitializer<Channel> {

	private static final Logger log = LoggerFactory.getLogger(ServerChannelInitializer.class);

	public static final HandlerKey<ProxyHandler> PROXY = new HandlerKey<>("ProxyHandler");

	private static final CvidResponseHandler HANDLER_CVID = new CvidResponseHandler();
	private static final ManagementHandler HANDLER_MANAGEMENT = new ManagementHandler();
	private static final ProtobufDecoder HANDLER_PROTO_DECODER = new ProtobufDecoder(MSG.getDefaultInstance());
	private static final ProtobufEncoder HANDLER_PROTO_ENCODER = new ProtobufEncoder();
	private static final ProtobufVarint32LengthFieldPrepender HANDLER_PROTO_FRAME_ENCODER = new ProtobufVarint32LengthFieldPrepender();

	private final SslContext sslCtx;

	/**
	 * The server's CVID.
	 */
	private final int cvid;

	public ServerChannelInitializer(Consumer<ChannelConfig> configurator) {
		var config = new ChannelConfig();
		configurator.accept(config);

		cvid = config.cvid;
		if (config.sslBuilder != null) {
			try {
				sslCtx = config.sslBuilder.build();
			} catch (SSLException e) {
				throw new IllegalArgumentException(e);
			}
		} else {
			sslCtx = null;
		}
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		var connection = ConnectionStore.create(ch);
		ch.attr(ChannelConstant.HANDSHAKE_FUTURE).set(ch.eventLoop().newPromise());

		ChannelPipeline p = ch.pipeline();

		p.addLast(TRAFFIC.next(p), new ChannelTrafficShapingHandler(Config.TRAFFIC_INTERVAL.value().orElse(5000)));

		if (Config.TLS_ENABLED.value().orElse(true))
			p.addLast(TLS.next(p), sslCtx.newHandler(ch.alloc()));

		if (Config.TRAFFIC_RAW.value().orElse(false))
			p.addLast(LOG_RAW.next(p), new LoggingHandler(Connection.class));

		p.addLast(FRAME_DECODER.next(p), new ProtobufVarint32FrameDecoder());
		p.addLast(PROXY.next(p), new ProxyHandler(cvid));
		p.addLast(PROTO_DECODER.next(p), HANDLER_PROTO_DECODER);
		p.addLast(FRAME_ENCODER.next(p), HANDLER_PROTO_FRAME_ENCODER);
		p.addLast(PROTO_ENCODER.next(p), HANDLER_PROTO_ENCODER);

		if (Config.TRAFFIC_DECODED.value().orElse(false))
			p.addLast(LOG_DECODED.next(p), new LoggingHandler(Connection.class));

		// Add CVID handler
		p.addLast(CVID.next(p), HANDLER_CVID);

		p.addLast(ThreadStore.get("net.exelet"), RESPONSE.next(p), new ResponseHandler());

		p.addLast(ThreadStore.get("net.exelet"), EXELET.next(p), new ExeletHandler(connection));

		p.addLast(MANAGEMENT.next(p), HANDLER_MANAGEMENT);
	}

}
