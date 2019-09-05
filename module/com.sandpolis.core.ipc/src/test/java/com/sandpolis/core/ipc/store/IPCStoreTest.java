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
import com.sandpolis.core.instance.ConfigConstant;
import com.sandpolis.core.instance.PoolConstant;
import com.sandpolis.core.ipc.Connector;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;

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
			config.register(Executors.newCachedThreadPool(), PoolConstant.net.ipc.listener,
					PoolConstant.net.ipc.receptor);
		});

		Config.register(ConfigConstant.net.ipc.timeout, 1000);
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
