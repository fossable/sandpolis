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

import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.instance.Generator.LoopConfig;
import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.core.instance.store.provider.MemoryMapStoreProvider;
import com.sandpolis.core.net.Protocol;
import com.sandpolis.core.net.connection.ConnectionEvents.SockEstablishedEvent;
import com.sandpolis.core.net.connection.ConnectionEvents.SockLostEvent;
import com.sandpolis.core.net.connection.ConnectionStore.ConnectionStoreConfig;
import com.sandpolis.core.net.init.ClientChannelInitializer;
import com.sandpolis.core.net.loop.ConnectionLoop;
import com.sandpolis.core.net.network.NetworkStore;

import io.netty.bootstrap.Bootstrap;

/**
 * A store for managing direct connections in the network.
 *
 * @author cilki
 * @see NetworkStore
 * @since 5.0.0
 */
public final class ConnectionStore extends MapStore<Connection, ConnectionStoreConfig> {

	public static final Logger log = LoggerFactory.getLogger(ConnectionStore.class);

	/**
	 * The store's default channel initializer.
	 */
	private ClientChannelInitializer initializer;

	public ConnectionStore() {
		super(log);
	}

	public Optional<Connection> getByCvid(int cvid) {
		return stream().filter(connection -> connection.getRemoteCvid() == cvid).findFirst();
	}

	/**
	 * Establish a connection. The resulting {@link Connection} will be added to the
	 * store automatically.
	 *
	 * @param bootstrap The connection bootstrap
	 * @return A {@link ConnectionFuture} which will complete after the connection
	 *         is established
	 */
	public ConnectionFuture connect(Bootstrap bootstrap) {
		configureDefaults(bootstrap);

		return new ConnectionFuture(bootstrap.connect());
	}

	/**
	 * Make a single client-server connection attempt to the given remote socket.
	 *
	 * @param address     The IP address or DNS name
	 * @param port        The port number
	 * @param strictCerts Whether strict certificate checking is enabled
	 * @return The future of the action
	 */
	public ConnectionFuture connect(String address, int port, boolean strictCerts) {
		return connect(new Bootstrap().remoteAddress(address, port).handler(initializer));
	}

	/**
	 * Create a new {@link ConnectionLoop} with the given configuration. The loop is
	 * automatically started.
	 *
	 * @param config The connection loop configuration
	 * @return The new connection loop
	 */
	public ConnectionLoop connect(LoopConfig config) {
		Objects.requireNonNull(config);

		Bootstrap bootstrap = new Bootstrap().handler(initializer);
		configureDefaults(bootstrap);

		ConnectionLoop loop = new ConnectionLoop(config, bootstrap);
		loop.start();

		return loop;
	}

	private void configureDefaults(Bootstrap bootstrap) {
		Objects.requireNonNull(bootstrap);

		if (bootstrap.config().group() == null)
			bootstrap.group(ThreadStore.get("net.connection.outgoing"));
		if (bootstrap.config().channelFactory() == null)
			bootstrap.channel(Protocol.TCP.getChannel());
	}

	@Subscribe
	private void onSockEstablished(SockEstablishedEvent event) {
		add(event.get());
	}

	@Subscribe
	private void onSockLost(SockLostEvent event) {
		remove(event.get().getRemoteCvid());
	}

	@Override
	public void init(Consumer<ConnectionStoreConfig> configurator) {
		var config = new ConnectionStoreConfig();
		configurator.accept(config);

		register(this);
		initializer = config.initializer;

		provider.initialize();
	}

	public final class ConnectionStoreConfig extends StoreConfig {

		public ClientChannelInitializer initializer = new ClientChannelInitializer();

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(Connection.class, Connection::tag);
		}
	}

	public static final ConnectionStore ConnectionStore = new ConnectionStore();
}
