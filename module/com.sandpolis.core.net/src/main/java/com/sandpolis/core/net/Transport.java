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
package com.sandpolis.core.net;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * The transport used by Netty.
 */
public enum Transport {

	/**
	 * Linux native transport.
	 */
	EPOLL,

	/**
	 * BSD native transport.
	 */
	KQUEUE,

	/**
	 * Default Java transport.
	 */
	NIO;

	public static final Transport INSTANCE = getTransport();

	/**
	 * Get the transport type for this platform. The classpath availablity of the
	 * native transport module is also checked.
	 *
	 * @return The transport type for this platform
	 */
	private static Transport getTransport() {
		// TODO native transports
		return NIO;
	}

	/**
	 * Get the appropriate {@link ServerChannel} type for this {@code Transport}
	 * type.
	 *
	 * @return A {@code ServerChannel} class
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends ServerChannel> getServerSocketChannel() {
		try {
			switch (this) {
			case EPOLL:
				return (Class<? extends ServerChannel>) Class
						.forName("io.netty.channel.epoll.EpollServerSocketChannel");
			case KQUEUE:
				return (Class<? extends ServerChannel>) Class
						.forName("io.netty.channel.kqueue.KQueueServerSocketChannel");
			default:
				return NioServerSocketChannel.class;
			}
		} catch (ClassNotFoundException ignore) {
			return NioServerSocketChannel.class;
		}
	}

	/**
	 * Get the appropriate {@link EventLoopGroup} type for this {@code Transport}
	 * type.
	 *
	 * @return A new {@code EventLoopGroup} object
	 */
	public EventLoopGroup getEventLoopGroup() {
		try {
			switch (this) {
			case EPOLL:
				return (EventLoopGroup) Class.forName("io.netty.channel.epoll.EpollEventLoopGroup").getConstructor()
						.newInstance();
			case KQUEUE:
				return (EventLoopGroup) Class.forName("io.netty.channel.epoll.KQueueEventLoopGroup").getConstructor()
						.newInstance();
			default:
				return new NioEventLoopGroup();
			}
		} catch (Exception ignore) {
			return new NioEventLoopGroup();
		}
	}
}
