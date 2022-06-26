//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.user;

import static org.s7s.core.instance.state.STStore.STStore;
import static org.s7s.core.instance.connection.ConnectionStore.ConnectionStore;
import static org.s7s.core.server.user.UserStore.UserStore;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.s7s.core.instance.User.UserConfig;
import org.s7s.core.protocol.Message.MSG;
import org.s7s.core.instance.exelet.ExeletContext;

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
