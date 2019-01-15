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

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.util.Base64;

import javax.net.ssl.SSLException;

import com.google.common.primitives.Bytes;
import com.sandpolis.core.instance.Config;
import com.sandpolis.core.net.Exelet;
import com.sandpolis.core.net.handler.CvidResponseHandler;
import com.sandpolis.core.net.init.ChannelConstant;
import com.sandpolis.core.net.init.PipelineInitializer;
import com.sandpolis.server.exe.AuthExe;
import com.sandpolis.server.exe.GenExe;
import com.sandpolis.server.exe.GroupExe;
import com.sandpolis.server.exe.ListenerExe;
import com.sandpolis.server.exe.LoginExe;
import com.sandpolis.server.exe.ServerExe;
import com.sandpolis.server.exe.UserExe;
import com.sandpolis.server.net.handler.ProxyHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.DefaultPromise;

/**
 * This {@link ChannelInitializer} configures a {@link ChannelPipeline} for use
 * as a server connection.
 *
 * @author cilki
 * @since 5.0.0
 */
public class ServerInitializer extends PipelineInitializer {

	private static final CvidResponseHandler cvidHandler = new CvidResponseHandler();

	/**
	 * All server {@link Exelet} classes.
	 */
	@SuppressWarnings("unchecked")
	private static final Class<? extends Exelet>[] exelets = new Class[] { AuthExe.class, GenExe.class, GroupExe.class,
			ListenerExe.class, LoginExe.class, ServerExe.class, UserExe.class };

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
	 * Construct a {@code ServerInitializer} with a self-signed certificate.
	 */
	public ServerInitializer() {
		super(exelets);
	}

	/**
	 * Construct a {@code ServerInitializer} with the given certificate.
	 *
	 * @param cert The certificate
	 * @param key  The private key
	 */
	public ServerInitializer(byte[] cert, byte[] key) {
		super(exelets);
		if (cert == null && key == null)
			return;
		if (cert == null || key == null)
			throw new IllegalArgumentException();

		byte[] begin_cert = "-----BEGIN CERTIFICATE-----\n".getBytes();
		byte[] begin_key = "-----BEGIN PRIVATE KEY-----\n".getBytes();
		byte[] end_cert = "-----END CERTIFICATE-----\n".getBytes();
		byte[] end_key = "-----END PRIVATE KEY-----\n".getBytes();

		// Convert to PEM format if necessary
		this.cert = (Bytes.indexOf(cert, begin_cert) == 0) ? cert
				: Bytes.concat(begin_cert, Base64.getEncoder().encode(cert), end_cert);
		this.key = (Bytes.indexOf(key, begin_key) == 0) ? key
				: Bytes.concat(begin_key, Base64.getEncoder().encode(key), end_key);

	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		super.initChannel(ch);

		if (Config.getBoolean("net.tls")) {
			SslHandler ssl = getSslContext().newHandler(ch.alloc());
			ch.pipeline().addAfter("traffic", "ssl", ssl);
			ch.attr(ChannelConstant.HANDLER_SSL).set(ssl);
		}

		// Add CVID handler
		ch.pipeline().addBefore("exe", "cvid", cvidHandler);
		ch.attr(ChannelConstant.FUTURE_CVID).set(new DefaultPromise<>(ch.eventLoop()));

		// Add proxy handler
		ch.pipeline().addAfter("protobuf.frame_decoder", "proxy", new ProxyHandler());
	}

	public SslContext getSslContext() throws Exception {
		if (sslCtx == null && Config.getBoolean("net.tls")) {
			sslCtx = buildSslContext();

			// No point in keeping these around anymore
			cert = null;
			key = null;
		}

		return sslCtx;
	}

	private SslContext buildSslContext() throws CertificateException, SSLException {
		if (cert != null && key != null)
			return SslContextBuilder.forServer(new ByteArrayInputStream(cert), new ByteArrayInputStream(key)).build();

		// fallback to a self-signed certificate
		SelfSignedCertificate ssc = new SelfSignedCertificate();
		return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
	}

}
