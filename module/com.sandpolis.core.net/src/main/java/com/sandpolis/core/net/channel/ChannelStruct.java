//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.channel;

import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import com.sandpolis.core.foundation.ConfigStruct;
import com.sandpolis.core.foundation.util.CertUtil;
import com.sandpolis.core.net.Channel.ChannelTransportProtocol;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

@ConfigStruct
public final class ChannelStruct {

	/**
	 * The transport-level protocol.
	 */
	public ChannelTransportProtocol transport = ChannelTransportProtocol.TCP;

	/**
	 * An optional CVID to use during the CVID handshake.
	 */
	public int cvid;

	public boolean requirePeerCertificate;

	public SslContextBuilder sslBuilder;

	public void serverTlsSelfSigned() {
		sslBuilder = SslContextBuilder.forServer(CertUtil.getDefaultKey(), CertUtil.getDefaultCert())
				.protocols("TLSv1.3");
	}

	public void serverTlsWithCert(byte[] certificate, byte[] key) {
		try {
			sslBuilder = SslContextBuilder.forServer(CertUtil.parseKey(key), CertUtil.parseCert(certificate))
					.protocols("TLSv1.3");
		} catch (InvalidKeySpecException | CertificateException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public void clientTlsInsecure() {
		sslBuilder = SslContextBuilder.forClient() //
				.trustManager(InsecureTrustManagerFactory.INSTANCE) //
				.protocols("TLSv1.3");
	}

	public void clientTlsVerifyCert() {
		sslBuilder = SslContextBuilder.forClient() //
				.trustManager(CertUtil.getServerRoot()) //
				.protocols("TLSv1.3");
	}
}
