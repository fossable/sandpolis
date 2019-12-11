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

import static com.sandpolis.core.instance.store.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.net.ChannelConstant;
import com.sandpolis.core.net.HandlerKey;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.ResponseHandler;
import com.sandpolis.core.net.handler.cvid.CvidResponseHandler;
import com.sandpolis.core.net.handler.exelet.ExeletHandler;
import com.sandpolis.core.net.init.AbstractChannelInitializer;
import com.sandpolis.core.net.plugin.ExeletProvider;
import com.sandpolis.core.net.sock.ServerSock;
import com.sandpolis.core.util.CertUtil;
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
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

/**
 * This {@link ChannelInitializer} configures a {@link ChannelPipeline} for use
 * as a server connection.
 *
 * @author cilki
 * @since 5.0.0
 */
public class ServerChannelInitializer extends AbstractChannelInitializer {

	private static final Logger log = LoggerFactory.getLogger(ServerChannelInitializer.class);

	private static final CvidResponseHandler HANDLER_CVID = new CvidResponseHandler();

	public static final HandlerKey<ChannelHandler> PROXY = new HandlerKey<>("ProxyHandler");

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
		super(TRAFFIC, SSL, LOG_RAW, FRAME_DECODER, PROXY, PROTO_DECODER, FRAME_ENCODER, PROTO_ENCODER, LOG_DECODED,
				CVID, RESPONSE, EXELET, MANAGEMENT);
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
		super.initChannel(ch);
		new ServerSock(ch);

		ChannelPipeline p = ch.pipeline();

		if (Config.getBoolean("net.connection.tls"))
			engage(p, SSL, getSslContext().newHandler(ch.alloc()));

		// Add proxy handler
		engage(p, PROXY, new ProxyHandler(cvid));

		// Add CVID handler
		engage(p, CVID, HANDLER_CVID);

		engage(p, RESPONSE, new ResponseHandler(), ThreadStore.get("net.exelet"));

		var exeletHandler = new ExeletHandler(ch.attr(ChannelConstant.SOCK).get(), exelets);
		engage(p, EXELET, exeletHandler, ThreadStore.get("net.exelet"));

		PluginStore.getLoadedPlugins().forEach(plugin -> {
			plugin.getExtensions(ExeletProvider.class).forEach(provider -> {
				exeletHandler.register(plugin.getId(), provider.getMessageType(), provider.getExelets());
			});
		});
	}

	private static final SslContextBuilder defaultContext = SslContextBuilder.forServer(CertUtil.getDefaultKey(),
			CertUtil.getDefaultCert());

	public SslContext getSslContext() throws Exception {
		if (sslCtx == null && Config.getBoolean("net.connection.tls")) {
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
