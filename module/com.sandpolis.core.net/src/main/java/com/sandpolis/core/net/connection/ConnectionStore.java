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
import com.sandpolis.core.instance.StateTree.VirtProfile.VirtConnection;
import com.sandpolis.core.instance.state.Document;
import com.sandpolis.core.instance.store.CollectionStore;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.core.instance.store.provider.MemoryMapStoreProvider;
import com.sandpolis.core.net.channel.ChannelConfig;
import com.sandpolis.core.net.channel.client.ClientChannelInitializer;
import com.sandpolis.core.net.connection.ConnectionEvents.SockLostEvent;
import com.sandpolis.core.net.connection.ConnectionStore.ConnectionStoreConfig;
import com.sandpolis.core.net.network.NetworkStore;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;

/**
 * {@link ConnectionStore} manages connections between the local instance and a
 * remote instance.
 *
 * @see NetworkStore
 * @since 5.0.0
 */
public final class ConnectionStore extends CollectionStore<Connection>
		implements ConfigurableStore<ConnectionStoreConfig> {

	public static final Logger log = LoggerFactory.getLogger(ConnectionStore.class);

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
		if (bootstrap.config().group() == null)
			bootstrap.group(ThreadStore.get("net.connection.outgoing"));

		return new ConnectionFuture(bootstrap.connect());
	}

	/**
	 * Make a single client-server connection attempt to the given remote socket.
	 *
	 * @param address The IP address or DNS name
	 * @param port    The port number
	 * @return The future of the action
	 */
	public ConnectionFuture connect(String address, int port) {
		return connect(address, port, config -> {
		});
	}

	/**
	 * Make a single client-server connection attempt to the given remote socket.
	 *
	 * @param address     The IP address or DNS name
	 * @param port        The port number
	 * @param strictCerts Whether strict certificate checking is enabled
	 * @return The future of the action
	 */
	public ConnectionFuture connect(String address, int port, Consumer<ChannelConfig> configurator) {
		return connect(
				new Bootstrap().remoteAddress(address, port).handler(new ClientChannelInitializer(configurator)));
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

		Bootstrap bootstrap = new Bootstrap().handler(new ClientChannelInitializer(builder -> {
		}));

		if (bootstrap.config().group() == null)
			bootstrap.group(ThreadStore.get("net.connection.outgoing"));

		ConnectionLoop loop = new ConnectionLoop(config, bootstrap);
		loop.start();

		return loop;
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

		provider.initialize();
	}

	public Connection create(Consumer<Connection> configurator) {
		return add(new Connection(new Document(null)), configurator);
	}

	public Connection create(Channel channel) {
		return create(connection -> {
			connection.setChannel(channel);
		});
	}

	public final class ConnectionStoreConfig extends StoreConfig {

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(Connection.class, Connection::tag, VirtConnection.COLLECTION);
		}
	}

	public static final ConnectionStore ConnectionStore = new ConnectionStore();
}
