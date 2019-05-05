/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
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
package com.sandpolis.core.ipc.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.store.pref.PrefStore;
import com.sandpolis.core.ipc.IPCStore;
import com.sandpolis.core.ipc.Listener;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;

class ListenerTest {

	private ExecutorService service;

	@BeforeEach
	private void init() {
		PrefStore.load(Instance.CHARCOAL, InstanceFlavor.NONE);
		IPCStore.init();

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
	@DisplayName("Check that the IPC port is reserved")
	void listen_2() throws IOException {
		try (Listener listener = new Listener()) {
			assertTrue(listener.getPort() > 0);

			// The port should be bound, so opening another socket should fail
			assertThrows(IOException.class, () -> new ServerSocket(listener.getPort()));
		}
	}
}
