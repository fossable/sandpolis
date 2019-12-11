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

import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public enum Protocol {
	TCP, UDP;

	@SuppressWarnings("unchecked")
	public Class<? extends Channel> getChannel() {
		switch (this) {
		case UDP:
			try {
				switch (Transport.INSTANCE) {
				case EPOLL:
					return (Class<? extends Channel>) Class.forName("io.netty.channel.epoll.EpollDatagramChannel");
				case KQUEUE:
					return (Class<? extends Channel>) Class.forName("io.netty.channel.kqueue.KQueueDatagramChannel");
				default:
					return NioDatagramChannel.class;
				}
			} catch (ClassNotFoundException ignore) {
				return NioDatagramChannel.class;
			}
		case TCP:
		default:
			try {
				switch (Transport.INSTANCE) {
				case EPOLL:
					return (Class<? extends Channel>) Class.forName("io.netty.channel.epoll.EpollSocketChannel");
				case KQUEUE:
					return (Class<? extends Channel>) Class.forName("io.netty.channel.kqueue.KQueueSocketChannel");
				default:
					return NioSocketChannel.class;
				}
			} catch (ClassNotFoundException ignore) {
				return NioSocketChannel.class;
			}
		}
	}
}
