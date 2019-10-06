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
package com.sandpolis.core.net.sock;

import static com.google.common.base.Preconditions.checkState;

import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLPeerUnverifiedException;

import com.sandpolis.core.net.ChannelConstant;
import com.sandpolis.core.net.HandlerKey;
import com.sandpolis.core.net.future.MessageFuture;
import com.sandpolis.core.net.init.AbstractChannelInitializer;
import com.sandpolis.core.net.util.CvidUtil;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;

public interface Sock {

	/**
	 * Get the {@link Channel} of this {@link Sock}.
	 *
	 * @return The underlying {@link Channel}
	 */
	public Channel channel();

	public default boolean isConnected() {
		return getHandshakeFuture().isDone() && channel().isActive();
	}

	public default boolean isAuthenticated() {
		return channel().attr(ChannelConstant.AUTH_STATE).get();
	}

	public default boolean isCertificateValid() {
		return channel().attr(ChannelConstant.CERTIFICATE_STATE).get();
	}

	public default void close() {
		channel().close();
		channel().eventLoop().shutdownGracefully();
	}

	/**
	 * Get the IP address of the remote host.
	 *
	 * @return The IPv4 address of the remote host
	 */
	public default String getRemoteIP() {
		checkState(isConnected());

		if (channel() instanceof EmbeddedChannel)
			// Don't fail during unit testing
			return channel().remoteAddress().toString();

		return ((InetSocketAddress) channel().remoteAddress()).getAddress().getHostAddress();
	}

	/**
	 * Get the remote port.
	 *
	 * @return The remote port to which the local host is connected
	 */
	public default int getRemotePort() {
		checkState(isConnected());

		return ((InetSocketAddress) channel().remoteAddress()).getPort();
	}

	/**
	 * Get the local port.
	 *
	 * @return The local port to which the remote host is connected
	 */
	public default int getLocalPort() {
		checkState(isConnected());

		return ((InetSocketAddress) channel().localAddress()).getPort();
	}

	/**
	 * Get the remote host's CVID.
	 *
	 * @return The CVID of the remote host
	 */
	public default int getRemoteCvid() {
		return channel().attr(ChannelConstant.CVID).get();
	}

	/**
	 * Get the remote host's UUID.
	 *
	 * @return The UUID of the remote host
	 */
	public default String getRemoteUuid() {
		return channel().attr(ChannelConstant.UUID).get();
	}

	/**
	 * Get the remote {@link Instance}.
	 *
	 * @return The instance type of the remote host
	 */
	public default Instance getRemoteInstance() {
		return CvidUtil.extractInstance(getRemoteCvid());
	}

	/**
	 * Get the remote {@link InstanceFlavor}.
	 *
	 * @return The instance flavor of the remote host
	 */
	public default InstanceFlavor getRemoteInstanceFlavor() {
		return CvidUtil.extractInstanceFlavor(getRemoteCvid());
	}

	/**
	 * Get the remote host's SSL certificate.
	 *
	 * @return The remote host's certificate
	 * @throws SSLPeerUnverifiedException
	 */
	public default X509Certificate getRemoteCertificate() throws SSLPeerUnverifiedException {
		checkState(isConnected());

		SslHandler ssl = getHandler(AbstractChannelInitializer.SSL);
		if (ssl == null)
			throw new SSLPeerUnverifiedException("SSL is disabled");

		return (X509Certificate) ssl.engine().getSession().getPeerCertificates()[0];
	}

	/**
	 * Get a {@link MessageFuture} that will be triggered by the arrival of a
	 * {@link Message} with the given ID.
	 *
	 * @param id The ID of the desired {@link Message}
	 * @return A {@link MessageFuture}
	 */
	public default MessageFuture read(int id) {
		return getHandler(AbstractChannelInitializer.RESPONSE).putResponseFuture(id, new MessageFuture());
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
	public default MessageFuture read(int id, long timeout, TimeUnit unit) {
		return getHandler(AbstractChannelInitializer.RESPONSE).putResponseFuture(id, new MessageFuture(timeout, unit));
	}

	/**
	 * Send a {@link Message} with the intention of receiving a reply.
	 *
	 * @param message The {@link Message} to send
	 * @return A {@link MessageFuture} which will be notified when the response is
	 *         received
	 */
	public default MessageFuture request(Message message) {
		MessageFuture future = read(message.getId());
		send(message);
		return future;
	}

	/**
	 * Send a {@link Message} with the intention of receiving a reply.
	 *
	 * @param message The {@link Message} to send
	 * @param timeout The response timeout
	 * @param unit    The timeout unit
	 * @return A {@link MessageFuture} which will be notified when the response is
	 *         received
	 */
	public default MessageFuture request(Message message, long timeout, TimeUnit unit) {
		MessageFuture future = read(message.getId(), timeout, unit);
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
	public default MessageFuture request(Message.Builder message) {
		if (message.getId() == 0)
			message.setId(0);// TODO GET FROM ID UTIL!
		return request(message.build());
	}

	/**
	 * Send a {@link Message} with the intention of receiving a reply.
	 *
	 * @param message The {@link Message} to send
	 * @param timeout The response timeout
	 * @param unit    The timeout unit
	 * @return A {@link MessageFuture} which will be notified when the response is
	 *         received
	 */
	public default MessageFuture request(Message.Builder message, long timeout, TimeUnit unit) {
		if (message.getId() == 0)
			message.setId(0);// TODO GET FROM ID UTIL!
		return request(message.build(), timeout, unit);
	}

	/**
	 * Write a {@link Message} and flush the {@link Channel}.
	 *
	 * @param message The {@link Message} to send
	 */
	public default void send(Message message) {
		channel().writeAndFlush(message);
	}

	/**
	 * An alias for: {@link #send(Message)}
	 */
	public default void send(Message.Builder message) {
		send(message.build());
	}

	/**
	 * Write a {@link Message} to the {@link Channel}, but do not flush it.
	 *
	 * @param m The {@link Message} to write
	 */
	public default void write(Message m) {
		channel().write(m);
	}

	/**
	 * A shortcut for {@link #write(Message)}.
	 */
	public default void write(Message.Builder m) {
		write(m.build());
	}

	/**
	 * Flush the underlying {@link Channel} immediately.
	 */
	public default void flush() {
		channel().flush();
	}

	/**
	 * Get the {@link ChannelTrafficShapingHandler} for the {@link Sock}.
	 *
	 * @return The associated {@link ChannelTrafficShapingHandler}
	 */
	public default ChannelTrafficShapingHandler getTrafficLimiter() {
		return getHandler(AbstractChannelInitializer.TRAFFIC);
	}

	/**
	 * Get the {@link TrafficCounter} for the {@link Sock}.
	 *
	 * @return The associated {@link TrafficCounter}
	 */
	public default TrafficCounter getTrafficInfo() {
		return getTrafficLimiter().trafficCounter();
	}

	public Future<Void> getHandshakeFuture();

	public default void authenticate() {
		checkState(isConnected());
		checkState(!isAuthenticated());

		getHandler(AbstractChannelInitializer.EXELET).authenticate();
		channel().attr(ChannelConstant.AUTH_STATE).set(true);
	}

	public default void deauthenticate() {
		checkState(isConnected());
		checkState(isAuthenticated());

		getHandler(AbstractChannelInitializer.EXELET).deauthenticate();
		channel().attr(ChannelConstant.AUTH_STATE).set(false);
	}

	@SuppressWarnings("unchecked")
	public default <E extends ChannelHandler> E getHandler(HandlerKey<E> key) {
		return (E) channel().pipeline().get(key.toString());
	}

	public default <E extends ChannelHandler> void engage(HandlerKey<E> type, E handler) {
		channel().pipeline().get(AbstractChannelInitializer.class).engage(channel().pipeline(), type, handler);
	}

	public default <E extends ChannelHandler> void engage(HandlerKey<E> type, E handler, EventExecutorGroup group) {
		channel().pipeline().get(AbstractChannelInitializer.class).engage(channel().pipeline(), type, handler, group);
	}
}
