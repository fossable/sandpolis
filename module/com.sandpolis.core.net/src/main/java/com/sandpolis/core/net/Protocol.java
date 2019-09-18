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
