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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.security.cert.CertificateException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.Config;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.util.SelfSignedCertificate;

class ServerInitializerTest {

	/**
	 * A ServerInitializer that uses an external certificate
	 */
	ServerChannelInitializer secure;

	/**
	 * A ServerInitializer that uses a fallback self-signed certificate
	 */
	ServerChannelInitializer fallback;

	@BeforeAll
	static void configure() {
		Config.register("logging.net.traffic.raw", false);
		Config.register("logging.net.traffic.decoded", false);
		Config.register("net.connection.tls", true);
		Config.register("traffic.interval", 4000);

		ThreadStore.init(config -> {
			config.ephemeral();
			config.defaults.put("net.exelet", new NioEventLoopGroup(2));
		});

		PluginStore.init(config -> {
			config.ephemeral();
		});
	}

	@BeforeEach
	void setup() throws CertificateException {
		SelfSignedCertificate ssc = new SelfSignedCertificate();
		secure = new ServerChannelInitializer(123, ssc.cert().getEncoded(), ssc.key().getEncoded());
		fallback = new ServerChannelInitializer(123);
	}

	@Test
	void testGetSslContext() throws Exception {

		// Ensure a non-null context is always returned
		assertNotNull(fallback.getSslContext());
		assertNotNull(secure.getSslContext());

		// Ensure the contexts don't change
		assertEquals(fallback.getSslContext(), fallback.getSslContext());
		assertEquals(secure.getSslContext(), secure.getSslContext());
	}

	@Test
	void testInitChannel() throws Exception {
		Method init = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
		init.setAccessible(true);

		EmbeddedChannel secureChannel = new EmbeddedChannel();
		EmbeddedChannel fallbackChannel = new EmbeddedChannel();

		// Use the initializers to build a channel
		init.invoke(fallback, fallbackChannel);
		init.invoke(secure, secureChannel);
	}
}
