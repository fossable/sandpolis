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
package com.sandpolis.core.net.init;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.HandlerKey.CVID;
import static com.sandpolis.core.net.HandlerKey.EXELET;
import static com.sandpolis.core.net.HandlerKey.FRAME_DECODER;
import static com.sandpolis.core.net.HandlerKey.FRAME_ENCODER;
import static com.sandpolis.core.net.HandlerKey.LOG_DECODED;
import static com.sandpolis.core.net.HandlerKey.LOG_RAW;
import static com.sandpolis.core.net.HandlerKey.MANAGEMENT;
import static com.sandpolis.core.net.HandlerKey.PROTO_DECODER;
import static com.sandpolis.core.net.HandlerKey.PROTO_ENCODER;
import static com.sandpolis.core.net.HandlerKey.RESPONSE;
import static com.sandpolis.core.net.HandlerKey.TLS;
import static com.sandpolis.core.net.HandlerKey.TRAFFIC;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.net.ChannelConstant;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.ManagementHandler;
import com.sandpolis.core.net.handler.ResponseHandler;
import com.sandpolis.core.net.handler.cvid.CvidRequestHandler;
import com.sandpolis.core.net.handler.exelet.ExeletHandler;
import com.sandpolis.core.net.sock.ClientSock;
import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.util.CertUtil;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

/**
 * This {@link ChannelInitializer} configures a {@link Channel} for connections
 * to the server.
 *
 * @author cilki
 * @since 5.0.0
 */
public class ClientChannelInitializer extends ChannelInitializer<Channel> {

	private static final CvidRequestHandler HANDLER_CVID = new CvidRequestHandler();
	private static final ManagementHandler HANDLER_MANAGEMENT = new ManagementHandler();
	private static final ProtobufDecoder HANDLER_PROTO_DECODER = new ProtobufDecoder(MSG.getDefaultInstance());
	private static final ProtobufEncoder HANDLER_PROTO_ENCODER = new ProtobufEncoder();
	private static final ProtobufVarint32LengthFieldPrepender HANDLER_PROTO_FRAME_ENCODER = new ProtobufVarint32LengthFieldPrepender();

	@SuppressWarnings("unchecked")
	private final Class<? extends Exelet>[] exelets;

	public ClientChannelInitializer() {
		this(new Class[] {});
	}

	public ClientChannelInitializer(Class<? extends Exelet>[] exelets) {
		this.exelets = exelets;
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		ch.attr(ChannelConstant.SOCK).set(new ClientSock(ch));
		ChannelPipeline p = ch.pipeline();

		p.addLast(TRAFFIC.next(p), new ChannelTrafficShapingHandler(Config.getInteger("traffic.interval")));

		if (Config.getBoolean("net.connection.tls")) {
			var ssl = SslContextBuilder.forClient();

			if (false) // TODO strict certs
				ssl.trustManager(CertUtil.getServerRoot());
			else
				ssl.trustManager(InsecureTrustManagerFactory.INSTANCE);

			p.addLast(TLS.next(p), ssl.build().newHandler(ch.alloc()));
		}

		if (Config.getBoolean("logging.net.traffic.raw"))
			p.addLast(LOG_RAW.next(p), new LoggingHandler(ClientSock.class));

		p.addLast(FRAME_DECODER.next(p), new ProtobufVarint32FrameDecoder());
		p.addLast(PROTO_DECODER.next(p), HANDLER_PROTO_DECODER);
		p.addLast(FRAME_ENCODER.next(p), HANDLER_PROTO_FRAME_ENCODER);
		p.addLast(PROTO_ENCODER.next(p), HANDLER_PROTO_ENCODER);

		if (Config.getBoolean("logging.net.traffic.raw"))
			p.addLast(LOG_DECODED.next(p), new LoggingHandler(ClientSock.class));

		p.addLast(CVID.next(p), HANDLER_CVID);

		p.addLast(ThreadStore.get("net.exelet"), RESPONSE.next(p), new ResponseHandler());

		p.addLast(ThreadStore.get("net.exelet"), EXELET.next(p),
				new ExeletHandler(ch.attr(ChannelConstant.SOCK).get(), exelets));

		p.addLast(MANAGEMENT.next(p), HANDLER_MANAGEMENT);
	}
}
