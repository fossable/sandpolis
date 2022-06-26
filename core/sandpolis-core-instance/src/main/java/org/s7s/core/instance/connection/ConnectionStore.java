//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.connection;

import static org.s7s.core.instance.thread.ThreadStore.ThreadStore;

import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.instance.state.InstanceOids.ProfileOid.ConnectionOid;
import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.store.ConfigurableStore;
import org.s7s.core.instance.store.STCollectionStore;
import org.s7s.core.instance.channel.ChannelStruct;
import org.s7s.core.instance.channel.client.ClientChannelInitializer;
import org.s7s.core.instance.connection.ConnectionStore.ConnectionStoreConfig;
import org.s7s.core.instance.network.NetworkStore;
import org.s7s.core.instance.util.ChannelUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;

/**
 * {@link ConnectionStore} manages connections between the local instance and a
 * remote instance.
 *
 * @see NetworkStore
 * @since 5.0.0
 */
public final class ConnectionStore extends STCollectionStore<Connection>
		implements ConfigurableStore<ConnectionStoreConfig> {

	public static final class ConnectionStoreConfig {

		public STDocument collection;
	}

	public static final record SockLostEvent(Connection connection) {
	}

	public static final record SockEstablishedEvent(Connection connection) {
	}

	public static final ConnectionStore ConnectionStore = new ConnectionStore();

	public static final Logger log = LoggerFactory.getLogger(ConnectionStore.class);

	public ConnectionStore() {
		super(log, Connection::new);
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
	 * Create and start a new {@link ConnectionLoop} with the given configuration.
	 *
	 * @param configurator The loop configuration
	 * @return A new connection loop
	 */
	public ConnectionLoop connect(Consumer<ConnectionLoop.ConfigStruct> configurator) {
		return new ConnectionLoop(configurator).start();
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
			config.clientTlsInsecure();
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
	public ConnectionFuture connect(String address, int port, Consumer<ChannelStruct> configurator) {
		var config = new ChannelStruct(configurator);

		return connect(new Bootstrap() //
				.remoteAddress(address, port) //
				.channel(ChannelUtil.getChannelType(config.transport)) //
				// TODO Pass struct instead of configurator
				.handler(new ClientChannelInitializer(configurator)));
	}

	public Connection create(Channel channel) {
		var connection = create(c -> {
		});
		connection.setChannel(channel);
		return connection;
	}

	public Optional<Connection> getBySid(int sid) {

		return values().stream().filter(connection -> {
			var attr = connection.get(ConnectionOid.REMOTE_SID);
			return attr.isPresent() && attr.asInt() == sid;
		}).findFirst();
	}

	@Override
	public void init(Consumer<ConnectionStoreConfig> configurator) {
		var config = new ConnectionStoreConfig();
		configurator.accept(config);

		setDocument(config.collection);

		register(this);
	}
}
