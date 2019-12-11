//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
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
