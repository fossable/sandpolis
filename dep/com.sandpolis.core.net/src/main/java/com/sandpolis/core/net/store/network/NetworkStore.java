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
package com.sandpolis.core.net.store.network;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.graph.MutableNetwork;
import com.google.common.graph.Network;
import com.google.common.graph.NetworkBuilder;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Store.ManualInitializer;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.future.MessageFuture;
import com.sandpolis.core.net.store.connection.ConnectionStore;
import com.sandpolis.core.proto.net.MCNetwork.EV_NetworkDelta;
import com.sandpolis.core.proto.net.MCNetwork.EV_NetworkDelta.LinkAdded;
import com.sandpolis.core.proto.net.MCNetwork.EV_NetworkDelta.LinkRemoved;
import com.sandpolis.core.proto.net.MCNetwork.EV_NetworkDelta.NodeAdded;
import com.sandpolis.core.proto.net.MCNetwork.EV_NetworkDelta.NodeRemoved;
import com.sandpolis.core.proto.net.MSG.Message;

/**
 * A static store for managing network connections, which may or may not be
 * directly connected and therefore present in the {@link ConnectionStore}.<br>
 * <br>
 * 
 * @author cilki
 * @see ConnectionStore
 * @since 5.0.0
 */
@ManualInitializer
public final class NetworkStore {
	private NetworkStore() {
	}

	private static final Logger log = LoggerFactory.getLogger(NetworkStore.class);

	/**
	 * The undirected graph which describes the visible connections between nodes on
	 * the network.
	 */
	private static MutableNetwork<Integer, SockLink> network;

	/**
	 * The CVID of the preferred server on the network.
	 */
	private static int preferredServer;

	public static void init() {
		network = NetworkBuilder.undirected().allowsSelfLoops(false).allowsParallelEdges(true).build();
	}

	public static void load(int cvid) {
		init();
		network.addNode(cvid);
	}

	/**
	 * Get an immutable representation of the network.
	 * 
	 * @return The underlying network graph of the store
	 */
	public static Network<Integer, SockLink> getNetwork() {
		return network;
	}

	/**
	 * Set the preferred server CVID.
	 * 
	 * @param cvid The new preferred server
	 */
	public static void setPreferredServer(int cvid) {
		preferredServer = cvid;
	}

	/**
	 * Update the network tree with the given delta. If the result of an operation
	 * is already present in the network (e.g. a node is already present and the
	 * operation is NodeAdd), then the operation is ignored.
	 * 
	 * @param delta The delta event that describes the change
	 */
	public static void updateNetwork(EV_NetworkDelta delta) {
		for (NodeAdded na : delta.getNodeAddedList())
			network.addNode(na.getCvid());
		for (NodeRemoved nr : delta.getNodeRemovedList())
			network.removeNode(nr.getCvid());

		for (LinkAdded la : delta.getLinkAddedList())
			network.addEdge(la.getCvid1(), la.getCvid2(), new SockLink(la.getLink()));
		for (LinkRemoved lr : delta.getLinkRemovedList())
			network.removeEdge(network.edgeConnectingOrNull(lr.getCvid1(), lr.getCvid2()));
	}

	/**
	 * Get the CVIDs of every node directly connected to the given CVID.
	 * 
	 * @param cvid The CVID
	 * @return A set of all directly connected CVIDs
	 */
	public static Set<Integer> getDirect(int cvid) {
		return network.adjacentNodes(cvid);
	}

	/**
	 * Get all links involving the given CVID.
	 * 
	 * @param cvid The CVID
	 * @return A set of all links involving the CVID
	 */
	public static Set<SockLink> getDirectLinks(int cvid) {
		return network.incidentEdges(cvid);
	}

	/**
	 * Get all links involving both given CVIDs.
	 * 
	 * @param cvid1 The first CVID
	 * @param cvid2 The second CVID
	 * @return A set of all links between the two CVIDs
	 */
	public static Set<SockLink> getDirectLinks(int cvid1, int cvid2) {
		return network.edgesConnecting(cvid1, cvid2);
	}

	/**
	 * Transmit a message into the network, taking a path through the preferred
	 * server.
	 * 
	 * @param message The message
	 * @return The next hop
	 */
	public static int deliver(Message message) {
		ConnectionStore.get(preferredServer).send(message);
		return preferredServer;
	}

	/**
	 * Transmit a message into the network, taking a path through the preferred
	 * server.
	 * 
	 * @param message The message
	 * @return The next hop
	 */
	public static int deliver(Message.Builder message) {
		return deliver(message.build());
	}

	/**
	 * Transmit a message into the network, taking the most direct path.
	 * 
	 * @param message The message
	 * @return The next hop
	 */
	public static int route(Message message) {
		if (network.adjacentNodes(Core.cvid()).contains(message.getTo())) {
			ConnectionStore.get(message.getTo()).send(message);
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
	public static int route(Message.Builder message) {
		return route(message.build());
	}

	/**
	 * Transmit a message into the network, taking the most direct path.
	 * 
	 * @param message The message
	 * @param timeout The message timeout
	 * @param unit    The timeout unit
	 * @return The next hop
	 */
	public static MessageFuture route(Message.Builder message, int timeout, TimeUnit unit) {
		int hop = route(message);
		return receive(hop, message.getId(), timeout, unit);
	}

	/**
	 * Receive a message from the given source.
	 * 
	 * @param cvid The message source
	 * @param id   The response ID
	 * @return A MessageFuture
	 */
	public static MessageFuture receive(int cvid, int id) {
		Sock sock = ConnectionStore.get(cvid);
		if (sock == null)
			return null;

		return sock.read(id);
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
	public static MessageFuture receive(int cvid, int id, int timeout, TimeUnit unit) {
		Sock sock = ConnectionStore.get(cvid);
		if (sock == null)
			return null;

		return sock.read(id, timeout, unit);
	}
}
