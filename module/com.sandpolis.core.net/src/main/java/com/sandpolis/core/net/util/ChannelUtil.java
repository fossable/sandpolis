package com.sandpolis.core.net.util;

import com.sandpolis.core.net.Channel.ChannelTransportImplementation;
import com.sandpolis.core.net.Channel.ChannelTransportProtocol;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public final class ChannelUtil {

	public static final ChannelTransportImplementation CURRENT_IMPL = discoverImplementation();

	private static ChannelTransportImplementation discoverImplementation() {
		try {
			Class.forName("io.netty.channel.epoll.EpollServerSocketChannel");
			return ChannelTransportImplementation.EPOLL;
		} catch (ClassNotFoundException e1) {
			try {
				Class.forName("io.netty.channel.epoll.EpollServerSocketChannel");
				return ChannelTransportImplementation.KQUEUE;
			} catch (ClassNotFoundException e2) {
				return ChannelTransportImplementation.NIO;
			}
		}
	}

	/**
	 * Build a new {@link EventLoopGroup}.
	 *
	 * @return A new {@link EventLoopGroup}
	 */
	public static EventLoopGroup newEventLoopGroup() {
		try {
			switch (CURRENT_IMPL) {
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

	/**
	 * Get the appropriate {@link ServerChannel} type.
	 * 
	 * @return A {@link ServerChannel} class
	 */
	@SuppressWarnings("unchecked")
	public static Class<? extends ServerChannel> getServerChannelType() {
		try {
			switch (CURRENT_IMPL) {
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
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static Class<? extends Channel> getChannelType(ChannelTransportProtocol protocol) {
		switch (protocol) {
		case UDP:
			try {
				switch (CURRENT_IMPL) {
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
				switch (CURRENT_IMPL) {
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

	private ChannelUtil() {
	}
}
