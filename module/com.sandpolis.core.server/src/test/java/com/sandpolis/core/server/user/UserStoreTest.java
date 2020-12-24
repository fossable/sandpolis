//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.server.user;

import static com.sandpolis.core.instance.state.STStore.STStore;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.server.user.UserStore.UserStore;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.User.UserConfig;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.exelet.ExeletContext;

import io.netty.channel.embedded.EmbeddedChannel;

@Disabled
class UserStoreTest {

	protected ExeletContext context;

	private static final UserConfig TEST_USER = UserConfig.newBuilder() //
			.setUsername("TESTUSER") //
			.setPassword("abc1234c") //
			.build();

	@BeforeEach
	void setup() throws URISyntaxException {
		STStore.init(config -> {
		});

		UserStore.init(config -> {
		});

		context = new ExeletContext(ConnectionStore.create(new EmbeddedChannel()), MSG.newBuilder().build());
	}

	@Test
	@DisplayName("Check basic usage of exists")
	void exists() {
		assertFalse(UserStore.getByUsername("TESTUSER").isPresent());
		UserStore.create(TEST_USER);
		assertTrue(UserStore.getByUsername("TESTUSER").isPresent());
		UserStore.removeValue(UserStore.getByUsername("TESTUSER").get());
		assertFalse(UserStore.getByUsername("TESTUSER").isPresent());
	}

}
