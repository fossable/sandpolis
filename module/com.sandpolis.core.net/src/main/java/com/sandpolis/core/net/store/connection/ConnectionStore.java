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
package com.sandpolis.core.net.store.connection;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;

import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.instance.Generator.LoopConfig;
import com.sandpolis.core.instance.storage.MemoryMapStoreProvider;
import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.instance.store.StoreBase.StoreConfig;
import com.sandpolis.core.net.Protocol;
import com.sandpolis.core.net.future.SockFuture;
import com.sandpolis.core.net.init.ClientChannelInitializer;
import com.sandpolis.core.net.loop.ConnectionLoop;
import com.sandpolis.core.net.sock.Sock;
import com.sandpolis.core.net.store.connection.ConnectionStore.ConnectionStoreConfig;
import com.sandpolis.core.net.store.network.NetworkStore;

import io.netty.bootstrap.Bootstrap;

/**
 * A static store for managing direct connections and connection attempt
 * threads.
 *
 * @author cilki
 * @see NetworkStore
 * @since 5.0.0
 */
public final class ConnectionStore extends MapStore<Integer, Sock, ConnectionStoreConfig> {

	public static final Logger log = LoggerFactory.getLogger(ConnectionStore.class);

	/**
	 * The store's default channel initializer.
	 */
	private ClientChannelInitializer initializer;

	public ConnectionStore() {
		super(log);
	}

	/**
	 * Establish a connection. The resulting {@link Sock} will be added to the store
	 * automatically.
	 *
	 * @param bootstrap The connection bootstrap
	 * @return A {@link SockFuture} which will complete after the connection is
	 *         established
	 */
	public SockFuture connect(Bootstrap bootstrap) {
		configureDefaults(bootstrap);

		return new SockFuture(bootstrap.connect());
	}

	/**
	 * Make a single client-server connection attempt to the given remote socket.
	 *
	 * @param address     The IP address or DNS name
	 * @param port        The port number
	 * @param strictCerts Whether strict certificate checking is enabled
	 * @return The future of the action
	 */
	public SockFuture connect(String address, int port, boolean strictCerts) {
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
	private void onSockEstablished(ConnectionStoreEvents.SockEstablishedEvent event) {
		add(event.get());
	}

	@Subscribe
	private void onSockLost(ConnectionStoreEvents.SockLostEvent event) {
		remove(event.get().getRemoteCvid());
	}

	@Override
	public ConnectionStore init(Consumer<ConnectionStoreConfig> configurator) {
		var config = new ConnectionStoreConfig();
		configurator.accept(config);

		register(this);
		initializer = config.initializer;

		return (ConnectionStore) super.init(null);
	}

	public final class ConnectionStoreConfig extends StoreConfig {

		public ClientChannelInitializer initializer = new ClientChannelInitializer();

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(Sock.class, Sock::getRemoteCvid);
		}
	}

	public static final ConnectionStore ConnectionStore = new ConnectionStore();
}
