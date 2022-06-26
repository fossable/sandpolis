//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.channel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.function.Consumer;

import org.s7s.core.foundation.S7SCertificate;
import org.s7s.core.protocol.Channel.ChannelTransportProtocol;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public final class ChannelStruct {

	/**
	 * The transport-level protocol.
	 */
	public ChannelTransportProtocol transport = ChannelTransportProtocol.TCP;

	/**
	 * An optional SID to use during the SID handshake.
	 */
	public int sid;

	public boolean requirePeerCertificate;

	public SslContextBuilder sslBuilder;

	public void serverTlsSelfSigned() {
		sslBuilder = SslContextBuilder
				.forServer(S7SCertificate.getDefaultKey(), S7SCertificate.getDefaultCert().certificate())
				.protocols("TLSv1.3");
	}

	public void serverTlsWithCert(byte[] certificate, byte[] key) {
		try (var keyOut = new ByteArrayInputStream(key); var certOut = new ByteArrayInputStream(certificate)) {
			sslBuilder = SslContextBuilder.forServer(keyOut, certOut).protocols("TLSv1.3");
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public void clientTlsInsecure() {
		sslBuilder = SslContextBuilder.forClient() //
				.trustManager(InsecureTrustManagerFactory.INSTANCE) //
				.protocols("TLSv1.3");
	}

	public void clientTlsVerifyCert() {
		try {
			sslBuilder = SslContextBuilder.forClient() //
					.trustManager(S7SCertificate.getServerRoot().certificate()) //
					.protocols("TLSv1.3");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public ChannelStruct(Consumer<ChannelStruct> configurator) {
		configurator.accept(this);
	}
}
