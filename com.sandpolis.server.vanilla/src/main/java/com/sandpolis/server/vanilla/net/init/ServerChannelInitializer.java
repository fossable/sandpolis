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
package com.sandpolis.server.vanilla.net.init;

import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.foundation.Config;
import com.sandpolis.core.net.ChannelConstant;
import com.sandpolis.core.net.HandlerKey;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.connection.ServerConnection;
import com.sandpolis.core.net.handler.ManagementHandler;
import com.sandpolis.core.net.handler.ResponseHandler;
import com.sandpolis.core.net.handler.cvid.CvidResponseHandler;
import com.sandpolis.core.net.handler.exelet.ExeletHandler;
import com.sandpolis.core.foundation.util.CertUtil;
import com.sandpolis.server.vanilla.exe.AuthExe;
import com.sandpolis.server.vanilla.exe.GenExe;
import com.sandpolis.server.vanilla.exe.GroupExe;
import com.sandpolis.server.vanilla.exe.ListenerExe;
import com.sandpolis.server.vanilla.exe.LoginExe;
import com.sandpolis.server.vanilla.exe.PluginExe;
import com.sandpolis.server.vanilla.exe.ServerExe;
import com.sandpolis.server.vanilla.exe.StreamExe;
import com.sandpolis.server.vanilla.exe.UserExe;
import com.sandpolis.server.vanilla.net.handler.ProxyHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

/**
 * This {@link ChannelInitializer} configures a {@link ChannelPipeline} for use
 * as a server connection.
 *
 * @author cilki
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

	/**
	 * All server {@link Exelet} classes.
	 */
	@SuppressWarnings("unchecked")
	private static final Class<? extends Exelet>[] exelets = new Class[] { AuthExe.class, GenExe.class, GroupExe.class,
			ListenerExe.class, LoginExe.class, ServerExe.class, UserExe.class, PluginExe.class, StreamExe.class };

	/**
	 * The certificate in PEM format.
	 */
	private byte[] cert;

	/**
	 * The private key in PEM format.
	 */
	private byte[] key;

	/**
	 * The cached {@link SslContext}.
	 */
	private SslContext sslCtx;

	/**
	 * The server's CVID.
	 */
	private int cvid;

	/**
	 * Construct a {@link ServerChannelInitializer} with a self-signed certificate.
	 *
	 * @param cvid The server CVID
	 */
	public ServerChannelInitializer(int cvid) {
		this.cvid = cvid;
	}

	/**
	 * Construct a {@link ServerChannelInitializer} with the given certificate.
	 *
	 * @param cvid The server CVID
	 * @param cert The certificate
	 * @param key  The private key
	 */
	public ServerChannelInitializer(int cvid, byte[] cert, byte[] key) {
		this(cvid);
		if (cert == null && key == null)
			return;
		if (cert == null || key == null)
			throw new IllegalArgumentException();

		this.cert = cert;
		this.key = key;
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		ch.attr(ChannelConstant.SOCK).set(new ServerConnection(ch));
		ChannelPipeline p = ch.pipeline();

		p.addLast(TRAFFIC.next(p), new ChannelTrafficShapingHandler(Config.TRAFFIC_INTERVAL.value().orElse(5000)));

		if (Config.TLS_ENABLED.value().orElse(true))
			p.addLast(TLS.next(p), getSslContext().newHandler(ch.alloc()));

		if (Config.TRAFFIC_RAW.value().orElse(false))
			p.addLast(LOG_RAW.next(p), new LoggingHandler(ServerConnection.class));

		p.addLast(FRAME_DECODER.next(p), new ProtobufVarint32FrameDecoder());
		p.addLast(PROXY.next(p), new ProxyHandler(cvid));
		p.addLast(PROTO_DECODER.next(p), HANDLER_PROTO_DECODER);
		p.addLast(FRAME_ENCODER.next(p), HANDLER_PROTO_FRAME_ENCODER);
		p.addLast(PROTO_ENCODER.next(p), HANDLER_PROTO_ENCODER);

		if (Config.TRAFFIC_DECODED.value().orElse(false))
			p.addLast(LOG_DECODED.next(p), new LoggingHandler(ServerConnection.class));

		// Add CVID handler
		p.addLast(CVID.next(p), HANDLER_CVID);

		p.addLast(ThreadStore.get("net.exelet"), RESPONSE.next(p), new ResponseHandler());

		p.addLast(ThreadStore.get("net.exelet"), EXELET.next(p),
				new ExeletHandler(ch.attr(ChannelConstant.SOCK).get(), exelets));

		p.addLast(MANAGEMENT.next(p), HANDLER_MANAGEMENT);
	}

	private static final SslContextBuilder defaultContext = SslContextBuilder.forServer(CertUtil.getDefaultKey(),
			CertUtil.getDefaultCert());

	public SslContext getSslContext() throws Exception {
		if (sslCtx == null && Config.TLS_ENABLED.value().orElse(true)) {
			if (cert != null && key != null) {
				sslCtx = SslContextBuilder.forServer(CertUtil.parseKey(key), CertUtil.parseCert(cert)).build();

				// No point in keeping these around anymore
				cert = null;
				key = null;
			} else {
				// Fallback certificate
				log.debug("Using default certificate");

				sslCtx = defaultContext.build();
			}
		}

		return sslCtx;
	}
}
