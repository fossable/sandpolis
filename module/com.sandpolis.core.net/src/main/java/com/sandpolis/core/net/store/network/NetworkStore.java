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
package com.sandpolis.core.net.store.network;

import static com.sandpolis.core.net.store.connection.ConnectionStore.ConnectionStore;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.Network;
import com.google.common.graph.NetworkBuilder;
import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.store.StoreBase;
import com.sandpolis.core.instance.store.StoreBase.StoreConfig;
import com.sandpolis.core.net.future.MessageFuture;
import com.sandpolis.core.net.store.connection.ConnectionStore;
import com.sandpolis.core.net.store.connection.ConnectionStoreEvents.SockEstablishedEvent;
import com.sandpolis.core.net.store.connection.ConnectionStoreEvents.SockLostEvent;
import com.sandpolis.core.net.store.network.NetworkStore.NetworkStoreConfig;
import com.sandpolis.core.net.store.network.NetworkStoreEvents.CvidChangedEvent;
import com.sandpolis.core.net.store.network.NetworkStoreEvents.ServerEstablishedEvent;
import com.sandpolis.core.net.store.network.NetworkStoreEvents.ServerLostEvent;
import com.sandpolis.core.net.util.CvidUtil;
import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.proto.net.MsgNetwork.EV_NetworkDelta;
import com.sandpolis.core.proto.net.MsgNetwork.EV_NetworkDelta.LinkAdded;
import com.sandpolis.core.proto.net.MsgNetwork.EV_NetworkDelta.LinkRemoved;
import com.sandpolis.core.proto.net.MsgNetwork.EV_NetworkDelta.NodeAdded;
import com.sandpolis.core.proto.net.MsgNetwork.EV_NetworkDelta.NodeRemoved;
import com.sandpolis.core.proto.util.Platform.Instance;

/**
 * A static store for managing network connections, which may or may not be
 * directly connected and therefore present in the {@link ConnectionStore}.<br>
 * <br>
 *
 * @author cilki
 * @see ConnectionStore
 * @since 5.0.0
 */
public final class NetworkStore extends StoreBase<NetworkStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(NetworkStore.class);

	public NetworkStore() {
		super(log);
	}

	/**
	 * The undirected graph which describes the visible connections between nodes on
	 * the network.
	 */
	private MutableNetwork<Integer, SockLink> network;

	/**
	 * The CVID of the preferred server on the network.
	 */
	private int preferredServer;

	@Subscribe
	private void onSockLost(SockLostEvent event) {
		network.edgeConnecting(Core.cvid(), event.get().getRemoteCvid()).ifPresent(network::removeEdge);

		// See if that was the last connection to a server
		if (event.get().getRemoteInstance() == Instance.SERVER) {
			// TODO
			post(ServerLostEvent::new, event.get().getRemoteCvid());
		}
	}

	@Subscribe
	private void onSockEstablished(SockEstablishedEvent event) {
		network.addNode(event.get().getRemoteCvid());
		// TODO add edge

		// See if that was the first connection to a server
		if (event.get().getRemoteInstance() == Instance.SERVER) {
			// TODO
			post(ServerEstablishedEvent::new, event.get().getRemoteCvid());
		}
	}

	@Subscribe
	private void onCvidChanged(CvidChangedEvent event) {
		network.addNode(event.get());
	}

	/**
	 * Get an immutable representation of the network.
	 *
	 * @return The underlying network graph of the store
	 */
	public Network<Integer, SockLink> getNetwork() {
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

	public int getPreferredServer() {
		if (preferredServer == 0)
			// Choose a server at random
			preferredServer = network.nodes().stream().filter(cvid -> CvidUtil.extractInstance(cvid) == Instance.SERVER)
					.findAny().orElse(0);

		return preferredServer;
	}

	/**
	 * Update the network tree with the given delta. If the result of an operation
	 * is already present in the network (e.g. a node is already present and the
	 * operation is NodeAdd), then the operation is ignored.
	 *
	 * @param delta The delta event that describes the change
	 */
	public void updateNetwork(EV_NetworkDelta delta) {
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
	public Set<Integer> getDirect(int cvid) {
		return network.adjacentNodes(cvid);
	}

	/**
	 * Get all links involving the given CVID.
	 *
	 * @param cvid The CVID
	 * @return A set of all links involving the CVID
	 */
	public Set<SockLink> getDirectLinks(int cvid) {
		return network.incidentEdges(cvid);
	}

	/**
	 * Get all links involving both given CVIDs.
	 *
	 * @param cvid1 The first CVID
	 * @param cvid2 The second CVID
	 * @return A set of all links between the two CVIDs
	 */
	public Set<SockLink> getDirectLinks(int cvid1, int cvid2) {
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
		ConnectionStore.get(getPreferredServer()).get().send(message);
		return getPreferredServer();
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
	public int route(MSG message) {
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
		int hop = findHop(message);
		if (!Config.has(timeoutClass))
			timeoutClass = "net.message.default_timeout";

		MessageFuture mf = receive(hop, message.getId(), Config.getInteger(timeoutClass), TimeUnit.MILLISECONDS);
		ConnectionStore.get(hop).get().send(message);
		return mf;
	}

	// TODO temporary
	private int findHop(MSG.Builder message) {
		if (network.adjacentNodes(Core.cvid()).contains(message.getTo()))
			return message.getTo();

		return getPreferredServer();
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
	public NetworkStore init(Consumer<NetworkStoreConfig> configurator) {
		var config = new NetworkStoreConfig();
		configurator.accept(config);

		preferredServer = config.preferredServer;
		network.addNode(config.cvid);

		ConnectionStore.register(this);
		post(CvidChangedEvent::new, config.cvid);

		return (NetworkStore) super.init(null);
	}

	public final class NetworkStoreConfig extends StoreConfig {

		public int preferredServer;
		public int cvid;

		@Override
		public void ephemeral() {
			network = NetworkBuilder.undirected().allowsSelfLoops(false).allowsParallelEdges(true).build();
		}

	}

	public static final NetworkStore NetworkStore = new NetworkStore();
}
