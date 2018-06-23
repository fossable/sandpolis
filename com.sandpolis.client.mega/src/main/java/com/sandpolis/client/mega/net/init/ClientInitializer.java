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
package com.sandpolis.client.mega.net.init;

import com.sandpolis.core.instance.Instance;
import com.sandpolis.core.net.Exelet;
import com.sandpolis.core.net.init.PipelineInitializer;
import com.sandpolis.core.util.CertUtil;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

/**
 * This {@link ChannelInitializer} configures a {@link ChannelPipeline} for use
 * as a client connection.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class ClientInitializer extends PipelineInitializer {

	/**
	 * All client {@link Exelet} classes.
	 */
	@SuppressWarnings("unchecked")
	private static final Class<? extends Exelet>[] exelets = new Class[] {};

	public ClientInitializer() {
		super(exelets, Instance.CLIENT);
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		super.initChannel(ch);
		CVID.initiateHandshake(ch, Instance.CLIENT);
	}

	@Override
	public SslContext getSslContext() throws Exception {
		return SslContextBuilder.forClient().trustManager(CertUtil.getRoot()).build();
	}

}