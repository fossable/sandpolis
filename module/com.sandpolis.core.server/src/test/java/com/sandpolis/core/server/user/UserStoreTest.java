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
package com.sandpolis.core.server.user;

import static com.sandpolis.core.server.user.UserStore.UserStore;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.User.UserConfig;
import com.sandpolis.core.server.user.UserStore;

class UserStoreTest {

	private static final UserConfig TEST_USER = UserConfig.newBuilder().setUsername("TESTUSER").setPassword("abc1234c")
			.build();

	@BeforeEach
	void setup() throws URISyntaxException {
		UserStore.init(config -> {
			config.ephemeral();
		});
	}

	@Test
	@DisplayName("Check basic usage of exists")
	void exists() {
		assertFalse(UserStore.getByUsername("TESTUSER").isPresent());
		UserStore.add(TEST_USER);
		assertTrue(UserStore.getByUsername("TESTUSER").isPresent());
		UserStore.removeValue(UserStore.getByUsername("TESTUSER").get());
		assertFalse(UserStore.getByUsername("TESTUSER").isPresent());
	}

}
