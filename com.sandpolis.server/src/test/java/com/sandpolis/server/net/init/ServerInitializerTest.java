/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.server.net.init;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.lang.reflect.Method;
import java.security.cert.CertificateException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class ServerInitializerTest {

	private ServerInitializer secure;
	private ServerInitializer secure2;
	private ServerInitializer insecure;

	@Before
	public void setup() throws CertificateException, IOException {
		SelfSignedCertificate ssc = new SelfSignedCertificate();
		secure = new ServerInitializer(Files.toByteArray(ssc.certificate()), Files.toByteArray(ssc.privateKey()));
		secure2 = new ServerInitializer(ssc.cert().getEncoded(), ssc.key().getEncoded());
		insecure = new ServerInitializer();
	}

	@Test
	public void testGetSslContext() throws Exception {
		assertNotNull(insecure.getSslContext());
		assertEquals(insecure.getSslContext(), insecure.getSslContext());
		assertNotNull(secure.getSslContext());
		assertEquals(secure.getSslContext(), secure.getSslContext());
		assertNotNull(secure2.getSslContext());
		assertEquals(secure2.getSslContext(), secure2.getSslContext());
	}

	@Test
	public void testInitChannel() throws Exception {
		Method init = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
		init.setAccessible(true);

		EmbeddedChannel insecureChannel = new EmbeddedChannel();
		EmbeddedChannel secureChannel = new EmbeddedChannel();
		EmbeddedChannel secure2Channel = new EmbeddedChannel();

		init.invoke(insecure, insecureChannel);
		init.invoke(secure, secureChannel);
		init.invoke(secure2, secure2Channel);

	}

}
