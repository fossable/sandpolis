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
package com.sandpolis.core.net.init;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.net.ChannelConstant;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.ResponseHandler;
import com.sandpolis.core.net.handler.cvid.CvidRequestHandler;
import com.sandpolis.core.net.handler.exelet.ExeletHandler;
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
	private static final Class<? extends Exelet>[] exelets = new Class[] {};

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
		engage(p, EXELET, new ExeletHandler(ch.attr(ChannelConstant.SOCK).get(), exelets),
				ThreadStore.get("net.exelet"));
	}
}
