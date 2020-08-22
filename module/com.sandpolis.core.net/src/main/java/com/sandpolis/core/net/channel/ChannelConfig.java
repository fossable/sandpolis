package com.sandpolis.core.net.channel;

import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import com.sandpolis.core.foundation.util.CertUtil;
import com.sandpolis.core.net.Channel.ChannelTransportProtocol;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public final class ChannelConfig {

	/**
	 * The transport-level protocol.
	 */
	public ChannelTransportProtocol transport;

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
		sslBuilder = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE);
	}

	public void clientTlsVerifyCert() {
		sslBuilder = SslContextBuilder.forClient().trustManager(CertUtil.getServerRoot());
	}
}
