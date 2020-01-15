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
package com.sandpolis.core.net.sock;

import static com.google.common.base.Preconditions.checkState;

import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.net.ssl.SSLPeerUnverifiedException;

import com.sandpolis.core.net.ChannelConstant;
import com.sandpolis.core.net.HandlerKey;
import com.sandpolis.core.net.future.MessageFuture;
import com.sandpolis.core.net.util.CvidUtil;
import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
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

		return (X509Certificate) getHandler(HandlerKey.TLS)
				.orElseThrow(() -> new SSLPeerUnverifiedException("SSL is disabled")).engine().getSession()
				.getPeerCertificates()[0];
	}

	/**
	 * Get a {@link MessageFuture} that will be triggered by the arrival of a
	 * {@link MSG} with the given ID.
	 *
	 * @param id The ID of the desired {@link MSG}
	 * @return A {@link MessageFuture}
	 */
	public default MessageFuture read(int id) {
		return getHandler(HandlerKey.RESPONSE).get().putResponseFuture(id, new MessageFuture());
	}

	/**
	 * Get a {@link MessageFuture} that will be triggered by the arrival of a
	 * {@link MSG} with the given ID.
	 *
	 * @param id      The ID of the desired {@link MSG}
	 * @param timeout The message timeout
	 * @param unit    The timeout unit
	 * @return A {@link MessageFuture}
	 */
	public default MessageFuture read(int id, long timeout, TimeUnit unit) {
		return getHandler(HandlerKey.RESPONSE).get().putResponseFuture(id, new MessageFuture(timeout, unit));
	}

	/**
	 * Send a {@link MSG} with the intention of receiving a reply.
	 *
	 * @param message The {@link MSG} to send
	 * @return A {@link MessageFuture} which will be notified when the response is
	 *         received
	 */
	public default MessageFuture request(MSG message) {
		MessageFuture future = read(message.getId());
		send(message);
		return future;
	}

	/**
	 * Send a {@link MSG} with the intention of receiving a reply.
	 *
	 * @param message The {@link MSG} to send
	 * @param timeout The response timeout
	 * @param unit    The timeout unit
	 * @return A {@link MessageFuture} which will be notified when the response is
	 *         received
	 */
	public default MessageFuture request(MSG message, long timeout, TimeUnit unit) {
		MessageFuture future = read(message.getId(), timeout, unit);
		send(message);
		return future;
	}

	/**
	 * Send a {@link MSG} with the intention of receiving a reply. The ID field will
	 * be populated if empty.
	 *
	 * @param message The {@link MSG} to send
	 * @return A {@link MessageFuture} which will be notified when the response is
	 *         received
	 */
	public default MessageFuture request(MSG.Builder message) {
		if (message.getId() == 0)
			message.setId(0);// TODO GET FROM ID UTIL!
		return request(message.build());
	}

	/**
	 * Send a {@link MSG} with the intention of receiving a reply.
	 *
	 * @param message The {@link MSG} to send
	 * @param timeout The response timeout
	 * @param unit    The timeout unit
	 * @return A {@link MessageFuture} which will be notified when the response is
	 *         received
	 */
	public default MessageFuture request(MSG.Builder message, long timeout, TimeUnit unit) {
		if (message.getId() == 0)
			message.setId(0);// TODO GET FROM ID UTIL!
		return request(message.build(), timeout, unit);
	}

	/**
	 * Write a {@link MSG} and flush the {@link Channel}.
	 *
	 * @param message The {@link MSG} to send
	 */
	public default void send(MSG message) {
		channel().writeAndFlush(message);
	}

	/**
	 * An alias for: {@link #send(MSG)}
	 */
	public default void send(MSG.Builder message) {
		send(message.build());
	}

	/**
	 * Write a {@link MSG} to the {@link Channel}, but do not flush it.
	 *
	 * @param m The {@link MSG} to write
	 */
	public default void write(MSG m) {
		channel().write(m);
	}

	/**
	 * A shortcut for {@link #write(MSG)}.
	 */
	public default void write(MSG.Builder m) {
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
		return getHandler(HandlerKey.TRAFFIC).get();
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

		getHandler(HandlerKey.EXELET).get().authenticate();
		channel().attr(ChannelConstant.AUTH_STATE).set(true);
	}

	public default void deauthenticate() {
		checkState(isConnected());
		checkState(isAuthenticated());

		getHandler(HandlerKey.EXELET).get().deauthenticate();
		channel().attr(ChannelConstant.AUTH_STATE).set(false);
	}

	public default <E extends ChannelHandler> Optional<E> getHandler(HandlerKey<E> key) {
		return getHandlers(key).findFirst();
	}

	@SuppressWarnings("unchecked")
	public default <E extends ChannelHandler> Stream<E> getHandlers(HandlerKey<E> key) {
		return (Stream<E>) channel().pipeline().names().stream()
				.filter(name -> key.base.equals(name.substring(0, name.indexOf('#')))).map(channel().pipeline()::get);
	}

	public default <E extends ChannelHandler> void engage(HandlerKey<E> type, E handler) {
		channel().pipeline().addBefore(HandlerKey.MANAGEMENT.base + "#0", type.next(channel().pipeline()), handler);
	}

	public default <E extends ChannelHandler> void engage(HandlerKey<E> type, E handler, EventExecutorGroup group) {
		channel().pipeline().addBefore(group, HandlerKey.MANAGEMENT.base + "#0", type.next(channel().pipeline()),
				handler);
	}

	public default void disengage(ChannelHandler handler) {
		channel().pipeline().remove(handler);
	}
}
