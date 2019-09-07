/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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

import static com.sandpolis.core.net.store.network.NetworkStore.NetworkStore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.proto.net.MCNetwork.EV_NetworkDelta;
import com.sandpolis.core.proto.net.MCNetwork.EV_NetworkDelta.LinkAdded;
import com.sandpolis.core.proto.net.MCNetwork.EV_NetworkDelta.LinkRemoved;
import com.sandpolis.core.proto.net.MCNetwork.EV_NetworkDelta.NodeAdded;
import com.sandpolis.core.proto.net.MCNetwork.EV_NetworkDelta.NodeRemoved;

class NetworkStoreTest {

	@BeforeEach
	void setup() {
		NetworkStore.init(config -> {
			config.ephemeral();
			config.preferredServer = 100;
			config.cvid = 123;
		});
	}

	@Test
	void testUpdateNetwork() {
		NetworkStore.updateNetwork(EV_NetworkDelta.newBuilder().addNodeAdded(NodeAdded.newBuilder().setCvid(100))
				.addNodeAdded(NodeAdded.newBuilder().setCvid(80)).addNodeAdded(NodeAdded.newBuilder().setCvid(40))
				.build());

		NetworkStore.updateNetwork(EV_NetworkDelta.newBuilder().addNodeAdded(NodeAdded.newBuilder().setCvid(9))
				.addLinkAdded(LinkAdded.newBuilder().setCvid1(123).setCvid2(100))
				.addLinkAdded(LinkAdded.newBuilder().setCvid1(123).setCvid2(40))
				.addLinkAdded(LinkAdded.newBuilder().setCvid1(40).setCvid2(80)).build());

		assertEquals(Set.of(123, 100, 80, 40, 9), NetworkStore.getNetwork().nodes());
		assertEquals(Set.of(100, 40), NetworkStore.getDirect(123));
		assertEquals(Set.of(), NetworkStore.getDirect(9));
		assertEquals(Set.of(40), NetworkStore.getDirect(80));
		assertEquals(Set.of(123, 80), NetworkStore.getDirect(40));

		NetworkStore.updateNetwork(
				EV_NetworkDelta.newBuilder().addNodeRemoved(NodeRemoved.newBuilder().setCvid(40)).build());
		assertEquals(Set.of(123, 100, 80, 9), NetworkStore.getNetwork().nodes());
		assertEquals(Set.of(100), NetworkStore.getDirect(123));
		assertEquals(Set.of(), NetworkStore.getDirect(80));
		assertThrows(IllegalArgumentException.class, () -> NetworkStore.getDirect(40));

		NetworkStore.updateNetwork(EV_NetworkDelta.newBuilder()
				.addLinkRemoved(LinkRemoved.newBuilder().setCvid1(123).setCvid2(100)).build());
		assertEquals(Set.of(123, 100, 80, 9), NetworkStore.getNetwork().nodes());
		assertEquals(Set.of(), NetworkStore.getDirect(123));
	}

}
