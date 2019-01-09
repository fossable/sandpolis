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
package com.sandpolis.viewer.cmd;

import com.sandpolis.core.net.Cmdlet;
import com.sandpolis.core.net.future.SockFuture;
import com.sandpolis.core.net.init.ClientPipelineInit;
import com.sandpolis.core.net.store.connection.ConnectionStore;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Contains network commands.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class NetworkCmd extends Cmdlet<NetworkCmd> {

	/**
	 * Attempt to connect to a Sandpolis listener.
	 * 
	 * @param address The IP address or DNS name
	 * @param port    The port number
	 * @return The future of the action
	 */
	public SockFuture connect(String address, int port) {
		return ConnectionStore.connect(new Bootstrap().channel(NioSocketChannel.class).group(new NioEventLoopGroup())
				.remoteAddress(address, port)
				// TODO use static pipeline initializer defined somewhere
				.handler(new ClientPipelineInit(new Class[] {})));
	}

	/**
	 * Prepare for an asynchronous command.
	 * 
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link NetworkCmd} can be invoked
	 */
	public static NetworkCmd async() {
		return new NetworkCmd();
	}

	private NetworkCmd() {
	}
}
