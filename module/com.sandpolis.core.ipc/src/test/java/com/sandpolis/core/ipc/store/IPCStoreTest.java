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
package com.sandpolis.core.ipc.store;

import static com.sandpolis.core.instance.store.pref.PrefStore.PrefStore;
import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.ipc.IPCStore.IPCStore;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.ipc.Connector;
import com.sandpolis.core.instance.Platform.Instance;
import com.sandpolis.core.instance.Platform.InstanceFlavor;

class IPCStoreTest {

	@BeforeEach
	private void init() {
		PrefStore.init(config -> {
			config.instance = Instance.CHARCOAL;
			config.flavor = InstanceFlavor.NONE;
		});
		IPCStore.init(config -> {
			config.ephemeral();
		});
		ThreadStore.init(config -> {
			config.ephemeral();
			config.defaults.put("net.ipc.listener", Executors.newCachedThreadPool());
			config.defaults.put("net.ipc.receptor", Executors.newCachedThreadPool());
		});
	}

	@Test
	@DisplayName("Open a listener and connect to it")
	void listen_connect_1() throws IOException {
		IPCStore.listen(Instance.CHARCOAL, InstanceFlavor.NONE);

		Connector connector = IPCStore.connect(Instance.CHARCOAL, InstanceFlavor.NONE);

		assertEquals(1, IPCStore.getConnectors().size());
		assertEquals(1, IPCStore.getListeners().size());

		connector.close();
		assertEquals(0, IPCStore.getConnectors().size());
	}
}
