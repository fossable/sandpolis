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

import static com.sandpolis.core.instance.store.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.net.ChannelConstant;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.ResponseHandler;
import com.sandpolis.core.net.handler.cvid.CvidRequestHandler;
import com.sandpolis.core.net.handler.exelet.ExeletHandler;
import com.sandpolis.core.net.plugin.ExeletProvider;
import com.sandpolis.core.net.sock.ClientSock;
import com.sandpolis.core.util.CertUtil;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

/**
 * This {@link AbstractChannelInitializer} configures a {@link Channel} for
 * connections to the server.
 *
 * @author cilki
 * @since 5.0.0
 */
public class ClientChannelInitializer extends AbstractChannelInitializer {

	private static final CvidRequestHandler HANDLER_CVID = new CvidRequestHandler();

	@SuppressWarnings("unchecked")
	private static Class<? extends Exelet>[] exelets = new Class[] {};

	// Temporary
	public static void setExelets(Class<? extends Exelet>[] exelets) {
		ClientChannelInitializer.exelets = exelets;
	}

	private final boolean strictCerts;

	public ClientChannelInitializer(boolean strictCerts) {
		this.strictCerts = strictCerts;
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		super.initChannel(ch);
		new ClientSock(ch);

		ChannelPipeline p = ch.pipeline();

		if (Config.getBoolean("net.connection.tls")) {
			var ssl = SslContextBuilder.forClient();

			if (strictCerts)
				ssl.trustManager(CertUtil.getServerRoot());
			else
				ssl.trustManager(InsecureTrustManagerFactory.INSTANCE);

			engage(p, SSL, ssl.build().newHandler(ch.alloc()));
		}

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
}
