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
package com.sandpolis.core.net.network;

import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;

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
import com.sandpolis.core.foundation.Config;
import com.sandpolis.core.foundation.ConfigStruct;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Metatypes.InstanceType;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.StoreBase;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.connection.ConnectionEvents.SockEstablishedEvent;
import com.sandpolis.core.net.connection.ConnectionEvents.SockLostEvent;
import com.sandpolis.core.net.connection.ConnectionStore;
import com.sandpolis.core.net.message.MessageFuture;
import com.sandpolis.core.net.network.NetworkEvents.CvidChangedEvent;
import com.sandpolis.core.net.network.NetworkEvents.ServerEstablishedEvent;
import com.sandpolis.core.net.network.NetworkEvents.ServerLostEvent;
import com.sandpolis.core.net.network.NetworkStore.NetworkStoreConfig;
import com.sandpolis.core.net.util.CvidUtil;

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
public final class NetworkStore extends StoreBase implements ConfigurableStore<NetworkStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(NetworkStore.class);

	public NetworkStore() {
		super(log);
	}

	/**
	 * The undirected graph which describes the visible connections between nodes on
	 * the network.
	 */
	private MutableNetwork<Integer, NetworkConnection> network;

	/**
	 * The CVID of the preferred server on the network.
	 */
	private int preferredServer;

	@Subscribe
	private synchronized void onSockLost(SockLostEvent event) {
		if (network.nodes().contains(Core.cvid()) && network.nodes().contains(event.get().getRemoteCvid()))
			network.edgeConnecting(Core.cvid(), event.get().getRemoteCvid()).ifPresent(network::removeEdge);

		// Remove nodes that are now disconnected
		network.nodes().stream().filter(cvid -> Core.cvid() != cvid).filter(cvid -> network.degree(cvid) == 0)
				.collect(Collectors.toUnmodifiableList()).forEach(network::removeNode);

		// See if that was the last connection to a server
		if (event.get().getRemoteInstance() == InstanceType.SERVER) {
			// TODO
			post(ServerLostEvent::new, event.get().getRemoteCvid());
		}
	}

	@Subscribe
	private synchronized void onSockEstablished(SockEstablishedEvent event) {
		network.addNode(event.get().getRemoteCvid());
		// TODO add edge

		// See if that was the first connection to a server
		if (event.get().getRemoteInstance() == InstanceType.SERVER) {
			// TODO
			post(ServerEstablishedEvent::new, event.get().getRemoteCvid());
		}
	}

	@Subscribe
	private synchronized void onCvidChanged(CvidChangedEvent event) {
		network.addNode(event.get());
	}

	/**
	 * Get an immutable representation of the network.
	 *
	 * @return The underlying network graph of the store
	 */
	public Network<Integer, NetworkConnection> getNetwork() {
		return network;
	}

	/**
	 * Explicitly set the preferred server CVID.
	 *
	 * @param cvid The new preferred server
	 */
	public void setPreferredServer(int cvid) {
		preferredServer = cvid;
	}

	public synchronized Optional<Integer> getPreferredServer() {

		if (!network.nodes().contains(preferredServer)) {
			// Choose a server at random
			var newServer = network.nodes().stream()
					.filter(cvid -> CvidUtil.extractInstance(cvid) == InstanceType.SERVER).findAny();
			if (newServer.isPresent()) {
				preferredServer = newServer.get();
				return newServer;
			} else {
				return Optional.empty();
			}
		}

		return Optional.of(preferredServer);
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
	 * Get the CVIDs of every node directly connected to the given CVID.
	 *
	 * @param cvid The CVID
	 * @return A set of all directly connected CVIDs
	 */
	public synchronized Set<Integer> getDirect(int cvid) {
		return network.adjacentNodes(cvid);
	}

	/**
	 * Get all links involving the given CVID.
	 *
	 * @param cvid The CVID
	 * @return A set of all links involving the CVID
	 */
	public synchronized Set<NetworkConnection> getDirectLinks(int cvid) {
		return network.incidentEdges(cvid);
	}

	/**
	 * Get all links involving both given CVIDs.
	 *
	 * @param cvid1 The first CVID
	 * @param cvid2 The second CVID
	 * @return A set of all links between the two CVIDs
	 */
	public synchronized Set<NetworkConnection> getDirectLinks(int cvid1, int cvid2) {
		return network.edgesConnecting(cvid1, cvid2);
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
		ConnectionStore.get(next).get().send(message);
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
	 * Transmit a message into the network, taking the most direct path.
	 *
	 * @param message The message
	 * @return The next hop
	 */
	public synchronized int route(MSG message) {
		if (network.adjacentNodes(Core.cvid()).contains(message.getTo())) {
			ConnectionStore.get(message.getTo()).get().send(message);
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
		if (network.adjacentNodes(Core.cvid()).contains(message.getTo())) {
			next = message.getTo();
		}

		// Try preferred server
		else {
			next = getPreferredServer().orElseThrow();
		}

		MessageFuture mf = receive(next, message.getId(), Config.MESSAGE_TIMEOUT.value().get(), TimeUnit.MILLISECONDS);
		ConnectionStore.get(next).get().send(message);
		return mf;
	}

	/**
	 * Receive a message from the given source.
	 *
	 * @param cvid The message source
	 * @param id   The response ID
	 * @return A MessageFuture
	 */
	public MessageFuture receive(int cvid, int id) {
		var sock = ConnectionStore.get(cvid);
		if (sock.isEmpty())
			return null;

		return sock.get().read(id);
	}

	/**
	 * Receive a message from the given source.
	 *
	 * @param cvid    The message source
	 * @param id      The message ID
	 * @param timeout The message timeout
	 * @param unit    The timeout unit
	 * @return A MessageFuture
	 */
	public MessageFuture receive(int cvid, int id, int timeout, TimeUnit unit) {
		var sock = ConnectionStore.get(cvid);
		if (sock.isEmpty())
			return null;

		return sock.get().read(id, timeout, unit);
	}

	@Override
	public void init(Consumer<NetworkStoreConfig> configurator) {
		var config = new NetworkStoreConfig();
		configurator.accept(config);

		preferredServer = config.preferredServer;
		network = NetworkBuilder.undirected().allowsSelfLoops(false).allowsParallelEdges(true).build();

		if (config.cvid != 0)
			network.addNode(config.cvid);

		ConnectionStore.register(this);
		post(CvidChangedEvent::new, config.cvid);
	}

	@ConfigStruct
	public static final class NetworkStoreConfig {

		public int preferredServer;
		public int cvid;
	}

	public static final NetworkStore NetworkStore = new NetworkStore();
}
