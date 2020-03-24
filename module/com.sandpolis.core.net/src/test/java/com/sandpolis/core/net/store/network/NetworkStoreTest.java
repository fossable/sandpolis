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
package com.sandpolis.core.net.store.network;

import static com.sandpolis.core.net.store.network.NetworkStore.NetworkStore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.net.MsgNetwork.EV_NetworkDelta;
import com.sandpolis.core.net.MsgNetwork.EV_NetworkDelta.LinkAdded;
import com.sandpolis.core.net.MsgNetwork.EV_NetworkDelta.LinkRemoved;
import com.sandpolis.core.net.MsgNetwork.EV_NetworkDelta.NodeAdded;
import com.sandpolis.core.net.MsgNetwork.EV_NetworkDelta.NodeRemoved;

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
