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

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;

import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import javax.net.ssl.SSLException;

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
import io.netty.handler.ssl.util.SelfSignedCertificate;

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
		engage(p, EXELET, new ExeletHandler(ch.attr(ChannelConstant.SOCK).get(), exelets),
				ThreadStore.get("net.exelet"));

	}

	public SslContext getSslContext() throws Exception {
		if (sslCtx == null && Config.getBoolean("net.connection.tls")) {
			sslCtx = buildSslContext();

			// No point in keeping these around anymore
			cert = null;
			key = null;
		}

		return sslCtx;
	}

	private SslContext buildSslContext() throws CertificateException, SSLException {
		if (cert != null && key != null) {
			try {
				return SslContextBuilder.forServer(CertUtil.parseKey(key), CertUtil.parseCert(cert)).build();
			} catch (InvalidKeySpecException e) {
				throw new RuntimeException(e);
			}
		}

		// Fallback certificate
		log.debug("Generating self-signed fallback certificate");
		SelfSignedCertificate ssc = new SelfSignedCertificate();
		return SslContextBuilder.forServer(ssc.key(), ssc.cert()).build();
	}

}
