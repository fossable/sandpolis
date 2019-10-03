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
package com.sandpolis.core.net.store.connection;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;

import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.storage.MemoryMapStoreProvider;
import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.net.Protocol;
import com.sandpolis.core.net.future.SockFuture;
import com.sandpolis.core.net.init.ClientChannelInitializer;
import com.sandpolis.core.net.loop.ConnectionLoop;
import com.sandpolis.core.net.sock.Sock;
import com.sandpolis.core.net.store.connection.ConnectionStore.ConnectionStoreConfig;
import com.sandpolis.core.net.store.network.NetworkStore;
import com.sandpolis.core.proto.util.Generator.LoopConfig;

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
		return connect(new Bootstrap().remoteAddress(address, port)
				// TODO use static pipeline initializer defined somewhere
				.handler(new ClientChannelInitializer(strictCerts)));
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

		Bootstrap bootstrap = new Bootstrap()
				// TODO use static pipeline initializer defined somewhere
				.handler(new ClientChannelInitializer(config.getStrictCerts()));
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

	@Override
	public ConnectionStore init(Consumer<ConnectionStoreConfig> configurator) {
		var config = new ConnectionStoreConfig();
		configurator.accept(config);

		return (ConnectionStore) super.init(null);
	}

	public final class ConnectionStoreConfig extends StoreConfig {

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(Sock.class, Sock::getRemoteCvid);
		}
	}

	public static final ConnectionStore ConnectionStore = new ConnectionStore();
}
