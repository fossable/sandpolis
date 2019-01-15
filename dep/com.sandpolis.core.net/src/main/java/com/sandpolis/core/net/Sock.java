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
package com.sandpolis.core.net;

import static com.sandpolis.core.net.Sock.ConnectionState.AUTHENTICATED;
import static com.sandpolis.core.net.Sock.ConnectionState.CONNECTED;
import static com.sandpolis.core.net.Sock.ConnectionState.NOT_CONNECTED;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Signaler;
import com.sandpolis.core.net.future.MessageFuture;
import com.sandpolis.core.net.init.ChannelConstant;
import com.sandpolis.core.net.store.connection.ConnectionStore.Events;
import com.sandpolis.core.proto.net.MCPing.RQ_Ping;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.util.IDUtil;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;

/**
 * This class wraps a {@link Channel} instance and provides helpful utilities.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class Sock {

	private static final Logger log = LoggerFactory.getLogger(Sock.class);

	/**
	 * The {@code Sock}'s underlying {@link Channel}.
	 */
	private Channel channel;

	/**
	 * Get the {@link Channel} of this {@link Sock}.
	 * 
	 * @return The {@code Sock}'s underlying {@link Channel}
	 */
	public Channel channel() {
		return channel;
	}

	/**
	 * Get the {@link Sock}'s {@link ConnectionState}.
	 * 
	 * @return The {@link ConnectionState}
	 */
	public ConnectionState getState() {
		return channel.attr(ChannelConstant.CONNECTION_STATE).get();
	}

	/**
	 * Get the {@link Sock}'s {@link CertificateState}.
	 * 
	 * @return The {@link CertificateState}
	 */
	public CertificateState getCertState() {
		return channel.attr(ChannelConstant.CERTIFICATE_STATE).get();
	}

	/**
	 * Change the Sock's connection state.
	 * 
	 * @param state The new connection state
	 */
	public void changeState(ConnectionState state) {
		var attribute = channel.attr(ChannelConstant.CONNECTION_STATE);
		if (attribute.get() == state)
			throw new IllegalArgumentException("Attempted to change state to itself: " + state);

		log.debug("[CVID {}] Connection state changed: {}->{}", getRemoteCvid(), attribute.get(), state);
		synchronized (attribute) {
			attribute.set(state);
		}

		switch (state) {
		case NOT_CONNECTED:
			Signaler.fire(Events.SOCK_LOST, this);
			break;
		default:
			break;
		}
	}

	/**
	 * Change the Sock's certificate state.
	 * 
	 * @param certState The new certificate state
	 */
	public void changeCertState(CertificateState certState) {
		var attribute = channel.attr(ChannelConstant.CERTIFICATE_STATE);
		if (attribute.get() == certState)
			throw new IllegalArgumentException();

		log.debug("[CVID {}] Certificate state changed: {}->{}", getRemoteCvid(), attribute.get(), certState);
		synchronized (attribute) {
			attribute.set(certState);
		}
	}

	/**
	 * Build a new {@link Sock} around the given {@link Channel}.
	 * 
	 * @param channel An active or inactive {@link Channel}
	 */
	public Sock(Channel channel) {
		this.channel = Objects.requireNonNull(channel);

		if (getState() == CONNECTED) {
			log.debug("[CVID {}] Connection state changed: {}->{}", getRemoteCvid(), NOT_CONNECTED, CONNECTED);
			Signaler.fire(Events.SOCK_ESTABLISHED, this);
		}
	}

	/**
	 * Shutdown the {@code Sock}.
	 */
	public void close() {
		requireState(CONNECTED, AUTHENTICATED);

		channel.close();
		channel.eventLoop().shutdownGracefully();
	}

	public void preauthenticate() {
		log.debug("Preauthenticating");
		channel.attr(ChannelConstant.HANDLER_EXECUTE).get().initUnauth(this);
	}

	public void authenticate() {
		log.debug("Authenticating");
		channel.attr(ChannelConstant.HANDLER_EXECUTE).get().initAuth(this);
	}

	public void deauthenticate() {
		log.debug("Deauthenticating");
		channel.attr(ChannelConstant.HANDLER_EXECUTE).get().resetHandlers();
	}

	/**
	 * Get the IP address of the remote host.
	 * 
	 * @return The IPv4 address of the remote host
	 */
	public String getRemoteIP() {
		requireState(CONNECTED, AUTHENTICATED);

		if (channel instanceof EmbeddedChannel)
			// For unit testing
			return channel.remoteAddress().toString();

		return ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
	}

	/**
	 * Get the remote port.
	 * 
	 * @return The remote port to which the local host is connected
	 */
	public int getRemotePort() {
		requireState(CONNECTED, AUTHENTICATED);

		return ((InetSocketAddress) channel.remoteAddress()).getPort();
	}

	/**
	 * Get the remote {@code CVID}.
	 * 
	 * @return The CVID of the remote host
	 */
	public int getRemoteCvid() {
		return channel.attr(ChannelConstant.CVID).get();
	}

	/**
	 * Get the remote {@link Instance}.
	 * 
	 * @return The instance type of the remote host
	 */
	public Instance getRemoteInstance() {
		return IDUtil.CVID.extractInstance(getRemoteCvid());
	}

	/**
	 * Get the local port.
	 * 
	 * @return The local port to which the remote host is connected
	 */
	public int getLocalPort() {
		requireState(CONNECTED, AUTHENTICATED);

		return ((InetSocketAddress) channel.localAddress()).getPort();
	}

	/**
	 * Get the remote host's SSL certificate.
	 * 
	 * @return The remote host's certificate
	 * @throws SSLPeerUnverifiedException
	 */
	public X509Certificate getRemoteCertificate() throws SSLPeerUnverifiedException {
		requireState(CONNECTED, AUTHENTICATED);

		SslHandler ssl = channel.attr(ChannelConstant.HANDLER_SSL).get();
		if (ssl == null)
			throw new SSLPeerUnverifiedException("SSL is disabled");

		return (X509Certificate) ssl.engine().getSession().getPeerCertificates()[0];
	}

	/**
	 * Get the {@link ChannelTrafficShapingHandler} for the {@link Sock}.
	 * 
	 * @return The associated {@link ChannelTrafficShapingHandler}
	 */
	public ChannelTrafficShapingHandler getTrafficLimiter() {
		return channel.attr(ChannelConstant.HANDLER_TRAFFIC).get();
	}

	/**
	 * Get the {@link TrafficCounter} for the {@link Sock}.
	 * 
	 * @return The associated {@link TrafficCounter}
	 */
	public TrafficCounter getTrafficInfo() {
		return getTrafficLimiter().trafficCounter();
	}

	/**
	 * Get the cumulative number of bytes received.
	 * 
	 * @return The count in bytes
	 */
	public long getRxBytes() {
		return getTrafficInfo().cumulativeReadBytes();
	}

	/**
	 * Get the cumulative number of bytes transmitted.
	 * 
	 * @return The count in bytes
	 */
	public long getTxBytes() {
		return getTrafficInfo().cumulativeWrittenBytes();
	}

	/**
	 * Get the current receieve speed.
	 * 
	 * @return The speed in bytes/second
	 */
	public long getRxSpeed() {
		return getTrafficInfo().lastReadThroughput();
	}

	/**
	 * Get the current transmit speed.
	 * 
	 * @return The speed in bytes/second
	 */
	public long getTxSpeed() {
		return getTrafficInfo().lastWriteThroughput();
	}

	/**
	 * Set a receive speed limit.
	 * 
	 * @param limit The limit in bytes/second
	 */
	public void setRxLimit(long limit) {
		getTrafficLimiter().setReadLimit(limit);
	}

	/**
	 * Set a transmit speed limit.
	 * 
	 * @param limit The limit in bytes/second
	 */
	public void setTxLimit(long limit) {
		getTrafficLimiter().setWriteLimit(limit);
	}

	/**
	 * Estimate the link latency by measuring how long it takes to receive a
	 * message.
	 * 
	 * @return The approximate time for a message to travel to the remote host in
	 *         milliseconds
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	public long ping() throws InterruptedException, ExecutionException, TimeoutException {
		requireState(CONNECTED, AUTHENTICATED);

		long t1 = System.nanoTime();
		request(Message.newBuilder().setRqPing(RQ_Ping.newBuilder())).get(2000, TimeUnit.MILLISECONDS);
		long t2 = System.nanoTime();

		// To get from 1e9 to (1e3)/2, multiply by (1e-6)/2 = 1/2000000
		return (t2 - t1) / 2000000;
	}

	/**
	 * Get a {@link MessageFuture} that will be triggered by the arrival of a
	 * {@link Message} with the given ID.
	 * 
	 * @param id The ID of the desired {@link Message}
	 * @return A {@link MessageFuture}
	 */
	public MessageFuture read(int id) {
		return channel.attr(ChannelConstant.HANDLER_EXECUTE).get().putResponseFuture(id, new MessageFuture());
	}

	/**
	 * Get a {@link MessageFuture} that will be triggered by the arrival of a
	 * {@link Message} with the given ID.
	 * 
	 * @param id      The ID of the desired {@link Message}
	 * @param timeout The message timeout
	 * @param unit    The timeout unit
	 * @return A {@link MessageFuture}
	 */
	public MessageFuture read(int id, int timeout, TimeUnit unit) {
		return channel.attr(ChannelConstant.HANDLER_EXECUTE).get().putResponseFuture(id,
				new MessageFuture(timeout, unit));
	}

	/**
	 * Send a {@link Message} with the intention of receiving a reply.
	 * 
	 * @param message The {@link Message} to send
	 * @return A {@link MessageFuture} which will be notified when the response is
	 *         received
	 */
	public MessageFuture request(Message message) {
		MessageFuture future = read(message.getId());
		send(message);
		return future;
	}

	/**
	 * Send a {@link Message} with the intention of receiving a reply. The ID field
	 * will be populated if empty.
	 * 
	 * @param message The {@link Message} to send
	 * @return A {@link MessageFuture} which will be notified when the response is
	 *         received
	 */
	public MessageFuture request(Message.Builder message) {
		if (message.getId() == 0)
			message.setId(0);// TODO GET FROM ID UTIL!
		return request(message.build());
	}

	/**
	 * Write a {@link Message} and flush the {@link Channel}.
	 * 
	 * @param message The {@link Message} to send
	 */
	public void send(Message message) {
		channel.writeAndFlush(message);
	}

	/**
	 * An alias for: {@link #send(Message)}
	 */
	public void send(Message.Builder message) {
		send(message.build());
	}

	/**
	 * Write a {@link Message} to the {@link Channel}, but do not flush it.
	 * 
	 * @param m The {@link Message} to write
	 */
	public void write(Message m) {
		channel.write(m);
	}

	/**
	 * A shortcut for {@link #write(Message)}.
	 */
	public void write(Message.Builder m) {
		write(m.build());
	}

	/**
	 * Flush the underlying {@link Channel} immediately.
	 */
	public void flush() {
		channel.flush();
	}

	@Override
	public int hashCode() {
		return channel.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Sock)
			return channel.equals(((Sock) obj).channel);
		return false;
	}

	/**
	 * Require that the {@link Sock} be in one of the given states. Otherwise throw
	 * an exception.
	 * 
	 * @param states A list of acceptable states
	 */
	private void requireState(ConnectionState... states) {
		ConnectionState state = getState();
		for (var s : states)
			if (state == s)
				return;
		throw new IllegalStateException("Unacceptable connection state: " + state);
	}

	public static enum Protocol {
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
						return (Class<? extends Channel>) Class
								.forName("io.netty.channel.kqueue.KQueueDatagramChannel");
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

	/**
	 * The transport used by Netty.
	 */
	public static enum Transport {

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
					return (EventLoopGroup) Class.forName("io.netty.channel.epoll.KQueueEventLoopGroup")
							.getConstructor().newInstance();
				default:
					return new NioEventLoopGroup();
				}
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException ignore) {
				return new NioEventLoopGroup();
			}
		}
	}

	public static enum ConnectionState {

		/**
		 * A {@code Sock} that is not and has never been connected.
		 */
		NOT_CONNECTED,

		/**
		 * A {@code Sock} that was connected, but has since closed. A {@code CLOSED}
		 * {@code Sock} may not be reopened.
		 */
		CLOSED,

		/**
		 * A {@code Sock} that is connected to the remote host, but not authenticated.
		 */
		CONNECTED,

		/**
		 * A {@code Sock} that is connected to and authenticated with the remote host.
		 */
		AUTHENTICATED;
	}

	public static enum CertificateState {

		/**
		 * The peer's certificate has been validated.
		 */
		VALID,

		/**
		 * The peer's certificate is either revoked, expired, invalid, self-signed, or
		 * missing. The {@code Sock} has nevertheless established a connection because
		 * strict certificate checking was disabled.
		 */
		INVALID,

		/**
		 * The peer's certificate is {@link #INVALID} and the {@code Sock} refused to
		 * proceed because strict certificate checking was enabled.
		 */
		REFUSED;
	}
}
