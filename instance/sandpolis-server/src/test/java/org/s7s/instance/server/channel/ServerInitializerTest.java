//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.channel;

import static org.s7s.core.instance.plugin.PluginStore.PluginStore;
import static org.s7s.core.instance.thread.ThreadStore.ThreadStore;

import java.lang.reflect.Method;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.util.SelfSignedCertificate;

@Disabled
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

		ThreadStore.init(config -> {
			config.defaults.put("net.exelet", new NioEventLoopGroup(2));
		});

		PluginStore.init(config -> {
		});
	}

	@BeforeEach
	void setup() throws CertificateException {
		SelfSignedCertificate ssc = new SelfSignedCertificate();
		secure = new ServerChannelInitializer(config -> {
			try {
				config.serverTlsWithCert(ssc.cert().getEncoded(), ssc.key().getEncoded());
			} catch (CertificateEncodingException e) {
				throw new RuntimeException(e);
			}
		});
		fallback = new ServerChannelInitializer(config -> {
		});
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
