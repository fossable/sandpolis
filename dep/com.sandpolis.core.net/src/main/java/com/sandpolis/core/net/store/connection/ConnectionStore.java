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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Store;
import com.sandpolis.core.instance.Store.AutoInitializer;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.future.SockFuture;
import com.sandpolis.core.net.loop.ConnectionLoop;
import com.sandpolis.core.net.store.network.NetworkStore;
import com.sandpolis.core.proto.net.MCNetwork.EV_NetworkDelta;
import com.sandpolis.core.proto.net.MCNetwork.EV_NetworkDelta.LinkAdded;
import com.sandpolis.core.proto.net.MCNetwork.EV_NetworkDelta.NodeAdded;
import com.sandpolis.core.proto.pojo.ConnectionLoop.LoopConfig;

import io.netty.bootstrap.Bootstrap;
import io.netty.util.concurrent.DefaultPromise;

/**
 * A static store for managing direct connections and connection attempt
 * threads.
 * 
 * @author cilki
 * @see NetworkStore
 * @since 5.0.0
 */
@AutoInitializer
public final class ConnectionStore extends Store {
	private ConnectionStore() {
	}

	public static final Logger log = LoggerFactory.getLogger(ConnectionStore.class);

	/**
	 * Stores direct connections between this instance and another.
	 */
	private static Map<Integer, Sock> connections;

	/**
	 * A list of connection threads that are currently attempting connections.
	 */
	private static List<ConnectionLoop> threads;

	static {
		init();
	}

	public static void init() {
		connections = new HashMap<>();
		threads = new LinkedList<>();
	}

	/**
	 * Add a connection to the store.
	 * 
	 * @param con The connection
	 */
	public static void add(Sock con) {
		if (con == null)
			throw new IllegalArgumentException();
		if (connections.containsKey(con.getRemoteCvid()))
			throw new IllegalArgumentException();
		log.debug("New sock registered with: {}", con.getRemoteCvid());

		connections.put(con.getRemoteCvid(), con);
		NetworkStore.updateNetwork(EV_NetworkDelta.newBuilder()
				.addNodeAdded(NodeAdded.newBuilder().setCvid(con.getRemoteCvid()))
				.addLinkAdded(LinkAdded.newBuilder().setCvid1(Core.cvid()).setCvid2(con.getRemoteCvid())).build());
	}

	/**
	 * Close and remove a connection from the store.
	 * 
	 * @param cvid The CVID of the connection to remove
	 * @return The closed connection or {@code null} if the connection was not found
	 */
	public static Sock close(int cvid) {
		Sock removal = connections.remove(cvid);
		if (removal != null) {
			removal.close();
			log.debug("Sock with: {} closed", cvid);

			NetworkStore.updateNetwork(
					EV_NetworkDelta.newBuilder().addNodeAdded(NodeAdded.newBuilder().setCvid(cvid)).build());
		}
		return removal;
	}

	/**
	 * Close and remove a connection from the store.
	 * 
	 * @param con The connection to remove
	 */
	public static void close(Sock con) {
		if (con == null)
			throw new IllegalArgumentException();

		close(con.getRemoteCvid());
	}

	/**
	 * Retrieve a connection from the store.
	 * 
	 * @param cvid The CVID to query
	 * @return The requested connection or {@code null} if the connection was not
	 *         found
	 */
	public static Sock get(int cvid) {
		return connections.get(cvid);
	}

	/**
	 * Get the number of connections in the store.
	 * 
	 * @return The connection count
	 */
	public static int getSize() {
		return connections.size();
	}

	/**
	 * Attempt to establish a peer-to-peer connection with the given instance. The
	 * server will coordinate the connection process.
	 * 
	 * @param cvid
	 * @return
	 */
	public static SockFuture connect(int cvid) {
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
	public static SockFuture connect(Bootstrap bootstrap) {
		if (bootstrap == null)
			throw new IllegalArgumentException();

		SockFuture sf = new SockFuture(bootstrap.connect());
		sf.addListener((DefaultPromise<Sock> future) -> {
			add(future.get());
		});

		return sf;
	}

	/**
	 * Create a new {@link ConnectionLoop} with the given configuration. The loop is
	 * automatically started.
	 * 
	 * @param config    The connection loop configuration
	 * @param bootstrap The connection bootstrap
	 * @return The new connection loop
	 */
	public static ConnectionLoop connect(LoopConfig config, Bootstrap bootstrap) {
		if (config == null)
			throw new IllegalArgumentException();
		if (bootstrap == null)
			throw new IllegalArgumentException();

		ConnectionLoop loop = new ConnectionLoop(config, bootstrap);
		threads.add(loop);
		return loop;
	}

}