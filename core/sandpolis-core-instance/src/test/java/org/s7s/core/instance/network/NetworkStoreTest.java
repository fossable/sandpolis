//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.network;

import static org.s7s.core.instance.network.NetworkStore.NetworkStore;

import org.junit.jupiter.api.BeforeEach;

class NetworkStoreTest {

	@BeforeEach
	void setup() {
		NetworkStore.init(config -> {
			config.preferredServer = 100;
			config.sid = 123;
		});
	}
}
