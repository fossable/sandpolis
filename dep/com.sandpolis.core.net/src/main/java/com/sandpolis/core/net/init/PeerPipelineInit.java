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

import com.sandpolis.core.net.Exelet;
import com.sandpolis.core.net.handler.HolePunchHandler;

import io.netty.channel.Channel;

/**
 * This {@link PipelineInitializer} configures a {@link Channel} for datagram
 * "connections" between two peers. The {@link Channel} will automatically
 * perform a NAT traversal prior to activation using the hole-punching
 * technique.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class PeerPipelineInit extends PipelineInitializer {

	public PeerPipelineInit(Class<? extends Exelet>[] exelets) {
		super(exelets);
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		super.initChannel(ch);

		ch.pipeline().addFirst(new HolePunchHandler());
	}

}
