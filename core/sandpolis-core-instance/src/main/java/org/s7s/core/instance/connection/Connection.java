//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.connection;

import static com.google.common.base.Preconditions.checkState;
import static org.s7s.core.instance.network.NetworkStore.NetworkStore;

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

import com.google.protobuf.MessageLiteOrBuilder;
import org.s7s.core.foundation.S7SRandom;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ConnectionOid;
import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.state.vst.AbstractSTDomainObject;
import org.s7s.core.protocol.Message.MSG;
import org.s7s.core.instance.channel.ChannelConstant;
import org.s7s.core.instance.channel.HandlerKey;
import org.s7s.core.instance.message.MessageFuture;
import org.s7s.core.instance.util.S7SMsg;
import org.s7s.core.instance.util.S7SSessionID;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;

/**
 * {@link Connection} manages a logical network "connection" between any two
 * arbitrary endpoints. If one out of the pair of hosts is the local instance,
 * this class provides send/receive capabilities.
 *
 * @since 5.0.0
 */
public class Connection extends AbstractSTDomainObject {

	private static final Logger log = LoggerFactory.getLogger(Connection.class);

	/**
	 * The underlying {@link Channel} which handles all I/O.
	 */
	private Channel channel;

	public Connection(STDocument document) {
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

		get(ConnectionOid.CERTIFICATE_VALID).source(channel().attr(ChannelConstant.CERTIFICATE_STATE)::get);
		get(ConnectionOid.AUTHENTICATED).source(channel().attr(ChannelConstant.AUTH_STATE)::get);
		get(ConnectionOid.CONNECTED)
				.source(() -> channel().attr(ChannelConstant.HANDSHAKE_FUTURE).get().isDone() && channel().isActive());
		get(ConnectionOid.CUMULATIVE_READ_BYTES).source(() -> {
			var trafficHandler = getTrafficHandler();
			if (trafficHandler.isPresent()) {
				return trafficHandler.get().trafficCounter().cumulativeReadBytes();
			}
			return -1L;
		});
		get(ConnectionOid.CUMULATIVE_WRITE_BYTES).source(() -> {
			var trafficHandler = getTrafficHandler();
			if (trafficHandler.isPresent()) {
				return trafficHandler.get().trafficCounter().cumulativeWrittenBytes();
			}
			return -1L;
		});
		get(ConnectionOid.READ_THROUGHPUT).source(() -> {
			var trafficHandler = getTrafficHandler();
			if (trafficHandler.isPresent()) {
				return trafficHandler.get().trafficCounter().lastReadThroughput();
			}
			return -1L;
		});
		get(ConnectionOid.WRITE_THROUGHPUT).source(() -> {
			var trafficHandler = getTrafficHandler();
			if (trafficHandler.isPresent()) {
				return trafficHandler.get().trafficCounter().lastWriteThroughput();
			}
			return -1L;
		});

		if (this.channel instanceof EmbeddedChannel) {
			get(ConnectionOid.REMOTE_ADDRESS).source(() -> {
				if (!get(ConnectionOid.CONNECTED).asBoolean())
					return null;

				return channel().remoteAddress().toString();
			});
		} else {
			get(ConnectionOid.REMOTE_ADDRESS).source(() -> {
				if (!get(ConnectionOid.CONNECTED).asBoolean())
					return null;

				return ((InetSocketAddress) channel().remoteAddress()).getAddress().getHostAddress();
			});
		}

		get(ConnectionOid.REMOTE_PORT).source(() -> {
			if (!get(ConnectionOid.CONNECTED).asBoolean())
				return null;

			return ((InetSocketAddress) channel().remoteAddress()).getPort();
		});

		get(ConnectionOid.LOCAL_PORT).source(() -> {
			if (!get(ConnectionOid.CONNECTED).asBoolean())
				return null;

			return ((InetSocketAddress) channel().localAddress()).getPort();
		});

		get(ConnectionOid.REMOTE_INSTANCE).source(() -> {
			if (!get(ConnectionOid.REMOTE_SID).isPresent())
				return null;

			return S7SSessionID.of(get(ConnectionOid.REMOTE_SID).asInt()).instanceType();
		});

		get(ConnectionOid.REMOTE_INSTANCE_FLAVOR).source(() -> {
			if (!get(ConnectionOid.REMOTE_SID).isPresent())
				return null;

			return S7SSessionID.of(get(ConnectionOid.REMOTE_SID).asInt()).instanceFlavor();
		});

		get(ConnectionOid.LOCAL_SID).source(NetworkStore::sid);
	}

	/**
	 * Transition this {@link Connection} into the authenticated state which enables
	 * it to handle messages that require authentication.
	 */
	public void authenticate() {
		checkState(get(ConnectionOid.CONNECTED).asBoolean());
		checkState(!get(ConnectionOid.AUTHENTICATED).asBoolean());

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
		checkState(get(ConnectionOid.CONNECTED).asBoolean());
		checkState(get(ConnectionOid.AUTHENTICATED).asBoolean());

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
		checkState(get(ConnectionOid.CONNECTED).asBoolean());

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
	public Optional<ChannelTrafficShapingHandler> getTrafficHandler() {
		return getHandler(HandlerKey.TRAFFIC);
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
	public <T> CompletionStage<T> request(Class<T> responseType, MessageLiteOrBuilder payload) {
		return request(S7SMsg.rq().pack(payload).setTo(get(ConnectionOid.REMOTE_SID).asInt())
				.setFrom(get(ConnectionOid.LOCAL_SID).asInt()).build()).toCompletionStage(responseType);
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
			message.setId(S7SRandom.nextNonzeroInt());
		return request(message.build(), timeout, unit);
	}

	/**
	 * Write a {@link MSG} and flush the {@link Channel}.
	 *
	 * @param message The {@link MSG} to send
	 */
	public ChannelFuture send(MSG message) {
		return channel().writeAndFlush(message);
	}

	/**
	 * An alias for: {@link #send(MSG)}
	 */
	public ChannelFuture send(MSG.Builder message) {
		return send(message.build());
	}

	/**
	 * Write a {@link MSG} to the {@link Channel}, but do not flush it.
	 *
	 * @param m The {@link MSG} to write
	 */
	public ChannelFuture write(MSG m) {
		return channel().write(m);
	}

	/**
	 * A shortcut for {@link #write(MSG)}.
	 */
	public ChannelFuture write(MSG.Builder m) {
		return write(m.build());
	}
}
