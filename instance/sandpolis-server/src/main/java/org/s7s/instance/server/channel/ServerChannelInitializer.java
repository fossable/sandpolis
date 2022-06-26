//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.channel;

import static org.s7s.core.instance.thread.ThreadStore.ThreadStore;
import static org.s7s.core.instance.channel.HandlerKey.SESSION;
import static org.s7s.core.instance.channel.HandlerKey.EXELET;
import static org.s7s.core.instance.channel.HandlerKey.FRAME_DECODER;
import static org.s7s.core.instance.channel.HandlerKey.FRAME_ENCODER;
import static org.s7s.core.instance.channel.HandlerKey.LOG_DECODED;
import static org.s7s.core.instance.channel.HandlerKey.LOG_RAW;
import static org.s7s.core.instance.channel.HandlerKey.MANAGEMENT;
import static org.s7s.core.instance.channel.HandlerKey.PROTO_DECODER;
import static org.s7s.core.instance.channel.HandlerKey.PROTO_ENCODER;
import static org.s7s.core.instance.channel.HandlerKey.RESPONSE;
import static org.s7s.core.instance.channel.HandlerKey.TLS;
import static org.s7s.core.instance.channel.HandlerKey.TRAFFIC;
import static org.s7s.core.instance.connection.ConnectionStore.ConnectionStore;

import java.util.function.Consumer;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.instance.InstanceContext;
import org.s7s.core.protocol.Message.MSG;
import org.s7s.core.instance.channel.ChannelConstant;
import org.s7s.core.instance.channel.ChannelStruct;
import org.s7s.core.instance.channel.HandlerKey;
import org.s7s.core.instance.connection.Connection;
import org.s7s.core.instance.session.SessionResponseHandler;
import org.s7s.core.instance.exelet.ExeletHandler;
import org.s7s.core.instance.handler.ManagementHandler;
import org.s7s.core.instance.handler.ResponseHandler;
import org.s7s.core.server.proxy.ProxyHandler;

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
 * connection to an agent or client.
 *
 * @since 5.0.0
 */
public class ServerChannelInitializer extends ChannelInitializer<Channel> {

	private static final Logger log = LoggerFactory.getLogger(ServerChannelInitializer.class);

	public static final HandlerKey<ProxyHandler> PROXY = new HandlerKey<>("ProxyHandler");

	private static final SessionResponseHandler HANDLER_SESSION = new SessionResponseHandler();
	private static final ManagementHandler HANDLER_MANAGEMENT = new ManagementHandler();
	private static final ProtobufDecoder HANDLER_PROTO_DECODER = new ProtobufDecoder(MSG.getDefaultInstance());
	private static final ProtobufEncoder HANDLER_PROTO_ENCODER = new ProtobufEncoder();
	private static final ProtobufVarint32LengthFieldPrepender HANDLER_PROTO_FRAME_ENCODER = new ProtobufVarint32LengthFieldPrepender();

	private final SslContext sslCtx;

	/**
	 * The server's SID.
	 */
	private final int sid;

	public ServerChannelInitializer(Consumer<ChannelStruct> configurator) {
		var config = new ChannelStruct(configurator);

		sid = config.sid;
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

		p.addLast(TRAFFIC.next(p), new ChannelTrafficShapingHandler(InstanceContext.TRAFFIC_INTERVAL.get()));

		if (InstanceContext.TLS_ENABLED.get())
			p.addLast(TLS.next(p), sslCtx.newHandler(ch.alloc()));

		if (InstanceContext.LOG_TRAFFIC_RAW.get())
			p.addLast(LOG_RAW.next(p), new LoggingHandler(Connection.class));

		p.addLast(FRAME_DECODER.next(p), new ProtobufVarint32FrameDecoder());
		p.addLast(PROXY.next(p), new ProxyHandler(sid));
		p.addLast(PROTO_DECODER.next(p), HANDLER_PROTO_DECODER);
		p.addLast(FRAME_ENCODER.next(p), HANDLER_PROTO_FRAME_ENCODER);
		p.addLast(PROTO_ENCODER.next(p), HANDLER_PROTO_ENCODER);

		if (InstanceContext.LOG_TRAFFIC_DECODED.get())
			p.addLast(LOG_DECODED.next(p), new LoggingHandler(Connection.class));

		// Add SID handler
		p.addLast(SESSION.next(p), HANDLER_SESSION);

		p.addLast(ThreadStore.get("net.exelet"), RESPONSE.next(p), new ResponseHandler());

		p.addLast(ThreadStore.get("net.exelet"), EXELET.next(p), new ExeletHandler(connection));

		p.addLast(MANAGEMENT.next(p), HANDLER_MANAGEMENT);
	}

}
