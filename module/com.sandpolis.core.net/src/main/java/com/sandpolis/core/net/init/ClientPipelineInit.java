/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.core.net.init;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.ConfigConstant.net;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.CvidRequestHandler;
import com.sandpolis.core.util.CertUtil;

import io.netty.channel.Channel;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.DefaultPromise;

/**
 * This {@link PipelineInitializer} configures a {@link Channel} for connections
 * to the server.
 *
 * @author cilki
 * @since 5.0.0
 */
public class ClientPipelineInit extends PipelineInitializer {

	private static final CvidRequestHandler cvidHandler = new CvidRequestHandler();

	public ClientPipelineInit(Class<? extends Exelet>[] exelets) {
		super(exelets);
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		super.initChannel(ch);

		if (Config.getBoolean(net.connection.tls)) {
			SslHandler ssl = SslContextBuilder.forClient().trustManager(CertUtil.getRoot()).build()
					.newHandler(ch.alloc());
			ch.pipeline().addAfter("traffic", "ssl", ssl);
			ch.attr(ChannelConstant.HANDLER_SSL).set(ssl);
		}

		ch.pipeline().addBefore("exe", "cvid", cvidHandler);
		ch.attr(ChannelConstant.FUTURE_CVID).set(new DefaultPromise<>(ch.eventLoop()));
	}

}
