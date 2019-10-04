/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.server.vanilla.net.init;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.security.cert.CertificateException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.store.thread.ThreadStore;

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

		ThreadStore.ThreadStore.init(config -> {
			config.ephemeral();
			config.defaults.put("net.exelet", new NioEventLoopGroup(2));
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
