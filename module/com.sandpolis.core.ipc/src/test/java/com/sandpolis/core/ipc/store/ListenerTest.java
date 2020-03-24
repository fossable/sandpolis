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
import static com.sandpolis.core.ipc.IPCStore.IPCStore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.ipc.Listener;
import com.sandpolis.core.instance.Platform.Instance;
import com.sandpolis.core.instance.Platform.InstanceFlavor;
import com.sandpolis.core.util.NetUtil;

class ListenerTest {

	private ExecutorService service;

	@BeforeEach
	private void init() {
		PrefStore.init(config -> {
			config.instance = Instance.CHARCOAL;
			config.flavor = InstanceFlavor.NONE;
		});
		IPCStore.init(config -> {
			config.ephemeral();
		});

		if (service != null)
			service.shutdownNow();
		service = Executors.newCachedThreadPool();
	}

	private void waitOnItem(List<?> list, int item, long timeout) throws InterruptedException {
		final long period = timeout / 10;

		while (timeout > 0) {
			if (list.size() - 1 == item)
				return;

			Thread.sleep(period);
			timeout -= period;
		}
	}

	@Test
	@DisplayName("Check that the IPC listener accepts connections")
	void listen_1() throws IOException, InterruptedException {
		try (Listener listener = new Listener()) {
			assertTrue(listener.getPort() > 0);

			// Start listener
			listener.start(service, service);

			try (Socket s = new Socket(InetAddress.getLoopbackAddress(), listener.getPort())) {
				waitOnItem(IPCStore.getReceptors(), 0, 1000);
				assertEquals(1, IPCStore.getReceptors().size());
			}

			waitOnItem(IPCStore.getReceptors(), -1, 5000);
			assertEquals(0, IPCStore.getReceptors().size());
		}
	}

	@Test
	@DisplayName("Check that the IPC port is opened")
	void listen_2() throws IOException {
		assertFalse(NetUtil.checkPort("127.0.0.1", 21023));

		try (Listener listener = new Listener(21023)) {
			assertTrue(NetUtil.checkPort("127.0.0.1", 21023));
		}

		assertFalse(NetUtil.checkPort("127.0.0.1", 21023));
	}

	@Test
	@DisplayName("Check that the IPC listener chooses a random port")
	void listen_3() throws IOException {
		try (Listener listener = new Listener()) {
			assertTrue(listener.getPort() > 0);
		}
	}
}
