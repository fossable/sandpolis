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
package com.sandpolis.server.vanilla.store.user;

import static com.sandpolis.server.vanilla.store.user.UserStore.UserStore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.proto.pojo.User.UserConfig;

class UserStoreTest {

	@BeforeEach
	void setup() throws URISyntaxException {
		UserStore.init(config -> {
			config.ephemeral();
		});
	}

	@Test
	@DisplayName("Check basic usage of exists")
	void exists() {
		assertFalse(UserStore.get("TESTUSER").isPresent());
		UserStore.add(UserConfig.newBuilder().setUsername("TESTUSER").setPassword("abc1234c"));
		assertTrue(UserStore.get("TESTUSER").isPresent());
		UserStore.remove("TESTUSER");
		assertFalse(UserStore.get("TESTUSER").isPresent());
	}

	@Test
	@DisplayName("Check basic usage of isExpired")
	void isExpired() {
		UserStore.add(UserConfig.newBuilder().setUsername("TESTUSER").setPassword("abc1234c"));
		assertFalse(UserStore.isExpired("TESTUSER"));
		UserStore.get("TESTUSER").get().setExpiration(System.currentTimeMillis() - 10000);
		assertTrue(UserStore.isExpired("TESTUSER"));
	}

	@Test
	@DisplayName("Check that a user can be added and retrieved")
	void add() {
		UserStore.add(UserConfig.newBuilder().setUsername("TESTUSER").setPassword("abc1234c"));

		User user = UserStore.get("TESTUSER").get();
		assertEquals(0, user.getExpiration());
		assertEquals("TESTUSER", user.getUsername());
		assertNotEquals(0, user.getCreation());
	}

}
