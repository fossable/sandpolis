//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.network;

import static com.sandpolis.core.net.network.NetworkStore.NetworkStore;

import org.junit.jupiter.api.BeforeEach;

class NetworkStoreTest {

	@BeforeEach
	void setup() {
		NetworkStore.init(config -> {
			config.preferredServer = 100;
			config.cvid = 123;
		});
	}
}
