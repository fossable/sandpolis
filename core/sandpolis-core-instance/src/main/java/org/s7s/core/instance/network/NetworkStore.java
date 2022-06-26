//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.network;

import static org.s7s.core.instance.connection.ConnectionStore.ConnectionStore;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.Network;
import com.google.common.graph.NetworkBuilder;
import org.s7s.core.instance.Entrypoint;
import org.s7s.core.instance.InstanceContext;
import org.s7s.core.foundation.Instance.InstanceType;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ConnectionOid;
import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.store.ConfigurableStore;
import org.s7s.core.instance.store.STCollectionStore;
import org.s7s.core.protocol.Message.MSG;
import org.s7s.core.instance.connection.Connection;
import org.s7s.core.instance.connection.ConnectionStore;
import org.s7s.core.instance.connection.ConnectionStore.SockEstablishedEvent;
import org.s7s.core.instance.connection.ConnectionStore.SockLostEvent;
import org.s7s.core.instance.message.MessageFuture;
import org.s7s.core.instance.network.NetworkStore.NetworkStoreConfig;
import org.s7s.core.instance.util.S7SSessionID;

/**
 * {@link NetworkStore} manages "logical" connections between any two instances
 * in the network.
 *
 * <p>
 * Internally, this class uses a graph to represent connections and is therefore
 * compatible with standard graph algorithms.
 *
 * @see ConnectionStore
 * @since 5.0.0
 */
public final class NetworkStore extends STCollectionStore<Connection> implements ConfigurableStore<NetworkStoreConfig> {

	public static final class NetworkStoreConfig {

		public int sid;
		public int preferredServer;
		public STDocument collection;

		private NetworkStoreConfig(Consumer<NetworkStoreConfig> configurator) {
			configurator.accept(this);
		}
	}

	public static final record ServerLostEvent(int sid) {
	}

	public static final record ServerEstablishedEvent(int sid) {
	}

	public static final record SidChangedEvent(int sid) {
	}

	private static final Logger log = LoggerFactory.getLogger(NetworkStore.class);

	public static final NetworkStore NetworkStore = new NetworkStore();

	/**
	 * The undirected graph which describes the visible connections between nodes on
	 * the network.
	 */
	private MutableNetwork<Integer, Connection> network;

	/**
	 * The SID of the preferred server on the network.
	 */
	private int preferredServer;

	/**
	 * The SID of this instance.
	 */
	private int sid;

	/**
	 * @return This instance's SID
	 */
	public int sid() {
		return sid;
	}

	public void setSid(int sid) {
		if (sid != 0) {
			network.removeNode(sid);
		}

		this.sid = sid;

		network.addNode(sid);
		post(new SidChangedEvent(sid));
	}

	public NetworkStore() {
		super(log, Connection::new);
	}

	/**
	 * Transmit a message into the network, taking a path through the preferred
	 * server.
	 *
	 * @param message The message
	 * @return The next hop
	 */
	public int deliver(MSG message) {
		int next = getPreferredServer().orElseThrow();
		ConnectionStore.getBySid(next).get().send(message);
		return next;
	}

	/**
	 * Transmit a message into the network, taking a path through the preferred
	 * server.
	 *
	 * @param message The message
	 * @return The next hop
	 */
	public int deliver(MSG.Builder message) {
		return deliver(message.build());
	}

	/**
	 * Get the SIDs of every node directly connected to the given SID.
	 *
	 * @param sid The SID
	 * @return A set of all directly connected SIDs
	 */
	public synchronized Set<Integer> getDirect(int sid) {
		return network.adjacentNodes(sid);
	}

	/**
	 * Get all links involving the given SID.
	 *
	 * @param sid The SID
	 * @return A set of all links involving the SID
	 */
	public synchronized Set<Connection> getDirectLinks(int sid) {
		return network.incidentEdges(sid);
	}

	/**
	 * Update the network tree with the given delta. If the result of an operation
	 * is already present in the network (e.g. a node is already present and the
	 * operation is NodeAdd), then the operation is ignored.
	 *
	 * @param delta The delta event that describes the change
	 */
//	public synchronized void updateNetwork(EV_NetworkDelta delta) {
//		for (NodeAdded na : delta.getNodeAddedList())
//			network.addNode(na.getCvid());
//		for (NodeRemoved nr : delta.getNodeRemovedList())
//			network.removeNode(nr.getCvid());
//
//		for (LinkAdded la : delta.getLinkAddedList())
//			network.addEdge(la.getCvid1(), la.getCvid2(), new SockLink(la.getLink()));
//		for (LinkRemoved lr : delta.getLinkRemovedList())
//			network.removeEdge(network.edgeConnectingOrNull(lr.getCvid1(), lr.getCvid2()));
//	}

	/**
	 * Get all links involving both given SIDs.
	 *
	 * @param cvid1 The first SID
	 * @param cvid2 The second SID
	 * @return A set of all links between the two SIDs
	 */
	public synchronized Set<Connection> getDirectLinks(int sid1, int sid2) {
		return network.edgesConnecting(sid1, sid2);
	}

	/**
	 * Get an immutable representation of the network.
	 *
	 * @return The underlying network graph of the store
	 */
	public Network<Integer, Connection> getNetwork() {
		return network;
	}

	public synchronized Optional<Integer> getPreferredServer() {

		if (!network.nodes().contains(preferredServer)) {
			// Choose a server at random
			var newServer = network.nodes().stream()
					.filter(sid -> S7SSessionID.of(sid).instanceType() == InstanceType.SERVER).findAny();
			if (newServer.isPresent()) {
				preferredServer = newServer.get();
				return newServer;
			} else {
				return Optional.empty();
			}
		}

		return Optional.of(preferredServer);
	}

	@Override
	public void init(Consumer<NetworkStoreConfig> configurator) {
		var config = new NetworkStoreConfig(configurator);

		preferredServer = config.preferredServer;
		network = NetworkBuilder.undirected().allowsSelfLoops(false).allowsParallelEdges(true).build();

		if (config.sid != 0) {
			sid = config.sid;
			network.addNode(config.sid);
		}

		ConnectionStore.register(this);
		post(new SidChangedEvent(config.sid));
	}

	@Subscribe
	private synchronized void onSockEstablished(SockEstablishedEvent event) {

		var remote_sid = event.connection().get(ConnectionOid.REMOTE_SID).asInt();

		// Add node if not already present
		if (!network.nodes().contains(remote_sid)) {
			log.debug("Adding node: {} ({})", remote_sid, S7SSessionID.of(remote_sid).instanceType());
			network.addNode(remote_sid);
		}

		// Add edge representing the new connection
		network.addEdge(sid(), remote_sid, event.connection());

		if (Entrypoint.data().instance() != InstanceType.SERVER) {
			// See if that was the first connection to a server
			if (event.connection().get(ConnectionOid.REMOTE_INSTANCE).asInstanceType() == InstanceType.SERVER) {
				// TODO
				postAsync(new ServerEstablishedEvent(remote_sid));
			}
		}
	}

	@Subscribe
	private synchronized void onSockLost(SockLostEvent event) {
		if (network.nodes().contains(sid())
				&& network.nodes().contains(event.connection().get(ConnectionOid.REMOTE_SID).asInt()))
			network.edgeConnecting(sid(), event.connection().get(ConnectionOid.REMOTE_SID).asInt())
					.ifPresent(network::removeEdge);

		// Remove nodes that are now disconnected
		network.nodes().stream().filter(sid -> sid() != sid).filter(sid -> network.degree(sid) == 0)
				.collect(Collectors.toUnmodifiableList()).forEach(network::removeNode);

		if (Entrypoint.data().instance() != InstanceType.SERVER) {
			// Check whether a server is still reachable after losing the connection
			for (var node : network.nodes()) {
				if (S7SSessionID.of(node).instanceType() == InstanceType.SERVER) {
					if (network.edgesConnecting(node, event.connection().get(ConnectionOid.REMOTE_SID).asInt())
							.size() > 0) {
						return;
					}
				}
			}

			// No servers are reachable
			postAsync(new ServerLostEvent(event.connection().get(ConnectionOid.REMOTE_SID).asInt()));
		}
	}

	/**
	 * Receive a message from the given source.
	 *
	 * @param sid The message source
	 * @param id  The response ID
	 * @return A MessageFuture
	 */
	public MessageFuture receive(int sid, int id) {
		var sock = ConnectionStore.getBySid(sid);
		if (sock.isEmpty())
			return null;

		return sock.get().read(id);
	}

	/**
	 * Receive a message from the given source.
	 *
	 * @param sid     The message source
	 * @param id      The message ID
	 * @param timeout The message timeout
	 * @param unit    The timeout unit
	 * @return A MessageFuture
	 */
	public MessageFuture receive(int sid, int id, int timeout, TimeUnit unit) {
		var sock = ConnectionStore.getBySid(sid);
		if (sock.isEmpty())
			return null;

		return sock.get().read(id, timeout, unit);
	}

	/**
	 * Transmit a message into the network, taking the most direct path.
	 *
	 * @param message The message
	 * @return The next hop
	 */
	public synchronized int route(MSG message) {
		if (network.adjacentNodes(sid()).contains(message.getTo())) {
			ConnectionStore.getBySid(message.getTo()).get().send(message);
			return message.getTo();
		} else {
			return deliver(message);
		}
	}

	/**
	 * Transmit a message into the network, taking the most direct path.
	 *
	 * @param message The message
	 * @return The next hop
	 */
	public int route(MSG.Builder message) {
		return route(message.build());
	}

	/**
	 * Transmit a message into the network, taking the most direct path.<br>
	 * <br>
	 * Implementation note: this method cannot use {@link #route(MSG)} because it
	 * must place the receive request before actually sending the message. (To avoid
	 * missing a message that is received extremely quickly).
	 *
	 * @param message      The message
	 * @param timeoutClass The message timeout class
	 * @return The next hop
	 */
	public MessageFuture route(MSG.Builder message, String timeoutClass) {
		int next;
		// TODO use timeout class

		// Search adjacent nodes first
		if (network.adjacentNodes(sid()).contains(message.getTo())) {
			next = message.getTo();
		}

		// Try preferred server
		else {
			next = getPreferredServer().orElseThrow();
		}

		MessageFuture mf = receive(next, message.getId(), InstanceContext.MESSAGE_TIMEOUT.get(), TimeUnit.MILLISECONDS);
		ConnectionStore.getBySid(next).get().send(message);
		return mf;
	}

	/**
	 * Explicitly set the preferred server SID.
	 *
	 * @param sid The new preferred server
	 */
	public void setPreferredServer(int sid) {
		preferredServer = sid;
	}
}
