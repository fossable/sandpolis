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
package com.sandpolis.core.net.connection;

import static com.google.common.base.Preconditions.checkState;

import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.StateTree.VirtProfile.VirtConnection;
import com.sandpolis.core.instance.state.Document;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.channel.ChannelConstant;
import com.sandpolis.core.net.channel.HandlerKey;
import com.sandpolis.core.net.message.MessageFuture;
import com.sandpolis.core.net.util.CvidUtil;
import com.sandpolis.core.net.util.MsgUtil;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;

/**
 * This class manages a logical network "connection" between any two arbitrary
 * endpoints. If one out of the pair of hosts is the local instance, this class
 * provides send/receive capabilities.
 *
 * @author cilki
 * @since 5.0.0
 */
public class Connection extends VirtConnection {

	private static final Logger log = LoggerFactory.getLogger(Connection.class);

	/**
	 * The underlying {@link Channel} which handles all I/O.
	 */
	private Channel channel;

	Connection(Document document) {
		super(document);
	}

	/**
	 * Build a new {@link Connection} around the given {@link Channel}.
	 *
	 * @param channel An active or inactive {@link Channel}
	 */
	void setChannel(Channel channel) {

		this.channel = Objects.requireNonNull(channel);

		channel.attr(ChannelConstant.SOCK).set(this);
		channel.attr(ChannelConstant.AUTH_STATE).set(false);
		channel.attr(ChannelConstant.CERTIFICATE_STATE).set(false);

		certificateValid().bind(() -> channel().attr(ChannelConstant.CERTIFICATE_STATE).get());
		authenticated().bind(() -> channel().attr(ChannelConstant.AUTH_STATE).get());
		connected().bind(() -> channel().attr(ChannelConstant.HANDSHAKE_FUTURE).get().isDone() && channel().isActive());
		cumulativeReadBytes().bind(() -> getTrafficHandler().trafficCounter().cumulativeReadBytes());
		cumulativeWriteBytes().bind(() -> getTrafficHandler().trafficCounter().cumulativeWrittenBytes());
		readThroughput().bind(() -> getTrafficHandler().trafficCounter().lastReadThroughput());
		writeThroughput().bind(() -> getTrafficHandler().trafficCounter().lastWriteThroughput());

		if (this.channel instanceof EmbeddedChannel) {
			remoteAddress().bind(() -> {
				if (!isConnected())
					return null;

				return channel().remoteAddress().toString();
			});
		} else {
			remoteAddress().bind(() -> {
				if (!isConnected())
					return null;

				return ((InetSocketAddress) channel().remoteAddress()).getAddress().getHostAddress();
			});
		}

		remoteCvid().bind(() -> {
			if (!isConnected())
				return null;

			return channel().attr(ChannelConstant.CVID).get();
		});

		remoteUuid().bind(() -> {
			if (!isConnected())
				return null;

			return channel().attr(ChannelConstant.UUID).get();
		});

		remotePort().bind(() -> {
			if (!isConnected())
				return null;

			return ((InetSocketAddress) channel().remoteAddress()).getPort();
		});

		localPort().bind(() -> {
			if (!isConnected())
				return null;

			return ((InetSocketAddress) channel().localAddress()).getPort();
		});

		remoteInstance().bind(() -> {
			if (!isConnected())
				return null;

			return CvidUtil.extractInstance(getRemoteCvid());
		});

		remoteInstanceFlavor().bind(() -> {
			if (!isConnected())
				return null;

			return CvidUtil.extractInstanceFlavor(getRemoteCvid());
		});
	}

	/**
	 * Transition this {@link Connection} into the authenticated state which enables
	 * it to handle messages that require authentication.
	 */
	public void authenticate() {
		checkState(isConnected());
		checkState(!isAuthenticated());

		channel().attr(ChannelConstant.AUTH_STATE).set(true);
	}

	/**
	 * Get the {@link Channel} of this {@link Connection}.
	 *
	 * @return The underlying {@link Channel}
	 */
	public Channel channel() {
		return channel;
	}

	/**
	 * Terminate the connection.
	 * 
	 * @return A future that will be notified when the connection is closed
	 */
	public Future<?> close() {
		channel().close();
		return channel().eventLoop().shutdownGracefully();
	}

	/**
	 * Transition this {@link Connection} into the unauthenticated state which
	 * prevents it from handling messages that require authentication.
	 */
	public void deauthenticate() {
		checkState(isConnected());
		checkState(isAuthenticated());

		channel().attr(ChannelConstant.AUTH_STATE).set(false);
	}

	/**
	 * Remove the given handler from the I/O pipeline.
	 * 
	 * @param handler The handler to remove
	 */
	public void disengage(ChannelHandler handler) {
		channel().pipeline().remove(handler);
	}

	public <E extends ChannelHandler> void engage(HandlerKey<E> type, E handler) {
		channel().pipeline().addBefore(HandlerKey.MANAGEMENT.base + "#0", type.next(channel().pipeline()), handler);
	}

	public <E extends ChannelHandler> void engage(HandlerKey<E> type, E handler, EventExecutorGroup group) {
		channel().pipeline().addBefore(group, HandlerKey.MANAGEMENT.base + "#0", type.next(channel().pipeline()),
				handler);
	}

	/**
	 * Flush the underlying {@link Channel} immediately.
	 */
	public void flush() {
		channel().flush();
	}

	public <E extends ChannelHandler> Optional<E> getHandler(HandlerKey<E> key) {
		return getHandlers(key).findFirst();
	}

	@SuppressWarnings("unchecked")
	public <E extends ChannelHandler> Stream<E> getHandlers(HandlerKey<E> key) {
		return (Stream<E>) channel().pipeline().names().stream()
				.filter(name -> key.base.equals(name.substring(0, name.indexOf('#')))).map(channel().pipeline()::get);
	}

	public X509Certificate getRemoteCertificate() throws SSLPeerUnverifiedException {
		checkState(isConnected());

		return (X509Certificate) getHandler(HandlerKey.TLS)
				.orElseThrow(() -> new SSLPeerUnverifiedException("SSL is disabled"))
				// Obtain the certificate from the SSL engine
				.engine().getSession().getPeerCertificates()[0];
	}

	/**
	 * Get the {@link ChannelTrafficShapingHandler} for the {@link Connection}.
	 *
	 * @return The associated {@link ChannelTrafficShapingHandler}
	 */
	public ChannelTrafficShapingHandler getTrafficHandler() {
		return getHandler(HandlerKey.TRAFFIC).get();
	}

	/**
	 * Get a {@link MessageFuture} that will be triggered by the arrival of a
	 * {@link MSG} with the given ID.
	 *
	 * @param id The ID of the desired {@link MSG}
	 * @return A {@link MessageFuture}
	 */
	public MessageFuture read(int id) {
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
	public MessageFuture read(int id, long timeout, TimeUnit unit) {
		return getHandler(HandlerKey.RESPONSE).get().putResponseFuture(id, new MessageFuture(timeout, unit));
	}

	/**
	 * Send a {@link MSG} with the intention of receiving a reply.
	 *
	 * @param message The {@link MSG} to send
	 * @return A {@link MessageFuture} which will be notified when the response is
	 *         received
	 */
	public MessageFuture request(MSG message) {
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
	public MessageFuture request(MSG message, long timeout, TimeUnit unit) {
		MessageFuture future = read(message.getId(), timeout, unit);
		send(message);
		return future;
	}

	/**
	 * Send a {@link MSG} with the intention of receiving a reply.
	 * 
	 * @param <E>          The expected response type
	 * @param responseType The expected response type
	 * @param payload      The request payload
	 * @return An asynchronous {@link CompletionStage}
	 */
	public <E extends Message> CompletionStage<E> request(Class<E> responseType, MessageOrBuilder payload) {
		return request(MsgUtil.rq(payload).setTo(getRemoteCvid()).setFrom(getLocalCvid()).build())
				.toCompletionStage(responseType);
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
	public MessageFuture request(MSG.Builder message, long timeout, TimeUnit unit) {
		if (message.getId() == 0)
			message.setId(0);// TODO GET FROM ID UTIL!
		return request(message.build(), timeout, unit);
	}

	/**
	 * Write a {@link MSG} and flush the {@link Channel}.
	 *
	 * @param message The {@link MSG} to send
	 */
	public void send(MSG message) {
		channel().writeAndFlush(message);
	}

	/**
	 * An alias for: {@link #send(MSG)}
	 */
	public void send(MSG.Builder message) {
		send(message.build());
	}

	/**
	 * Write a {@link MSG} to the {@link Channel}, but do not flush it.
	 *
	 * @param m The {@link MSG} to write
	 */
	public void write(MSG m) {
		channel().write(m);
	}

	/**
	 * A shortcut for {@link #write(MSG)}.
	 */
	public void write(MSG.Builder m) {
		write(m.build());
	}
}
