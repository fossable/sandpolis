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
package com.sandpolis.core.net.store.connection;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.Sock.Protocol;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.future.SockFuture;
import com.sandpolis.core.net.init.ClientPipelineInit;
import com.sandpolis.core.net.loop.ConnectionLoop;
import com.sandpolis.core.net.store.connection.ConnectionStore.ConnectionStoreConfig;
import com.sandpolis.core.net.store.network.NetworkStore;
import com.sandpolis.core.proto.util.Generator.LoopConfig;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

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
	 * Stores direct connections between this instance and another.
	 */
	private static final Map<Integer, Sock> connections = new HashMap<>();

	/**
	 * A list of connection threads that are currently attempting connections.
	 */
	private static final List<ConnectionLoop> threads = new LinkedList<>();

	@Override
	public void init(Consumer<ConnectionStoreConfig> c) {
		register(this);
	}

	public static final class ConnectionStoreConfig {

	}

	/**
	 * Connection events.
	 */
	public enum Events {

		/**
		 * Indicates that a connection has been lost.
		 */
		SOCK_LOST,

		/**
		 * Indicates that a new connection has been established.
		 */
		SOCK_ESTABLISHED;
	}

	@Subscribe
	private void onSockLost(Sock sock) {
		connections.remove(sock.getRemoteCvid());
	}

	@Subscribe
	private void onSockEstablished(Sock sock) {
		connections.put(sock.getRemoteCvid(), sock);
	}

	/**
	 * Attempt to establish a peer-to-peer connection with the given instance. The
	 * server will coordinate the connection process.
	 * 
	 * @param cvid
	 * @return
	 */
	public SockFuture connect(int cvid) {
		// TODO
		return null;
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
		Objects.requireNonNull(bootstrap);

		return new SockFuture(bootstrap.connect());
	}

	/**
	 * Create a new {@link ConnectionLoop} with the given configuration. The loop is
	 * automatically started.
	 * 
	 * @param config The connection loop configuration
	 * @return The new connection loop
	 */
	public ConnectionLoop connect(LoopConfig config, Class<? extends Exelet>[] exelets) {
		Objects.requireNonNull(config);

		// Build a bootstrap
		Bootstrap bootstrap = new Bootstrap().channel(Protocol.TCP.getChannel())
				.group(ThreadStore.get("net.connection.outgoing")).handler(new ClientPipelineInit(exelets));

		ConnectionLoop loop = new ConnectionLoop(config, bootstrap);
		loop.start();

		threads.add(loop);
		return loop;
	}

	/**
	 * Attempt to connect to a Sandpolis listener.
	 * 
	 * @param address The IP address or DNS name
	 * @param port    The port number
	 * @return The future of the action
	 */
	public SockFuture connect(String address, int port) {
		return connect(new Bootstrap().channel(NioSocketChannel.class).group(new NioEventLoopGroup())
				.remoteAddress(address, port)
				// TODO use static pipeline initializer defined somewhere
				.handler(new ClientPipelineInit(new Class[] {})));
	}

	public static final ConnectionStore ConnectionStore = new ConnectionStore();
}