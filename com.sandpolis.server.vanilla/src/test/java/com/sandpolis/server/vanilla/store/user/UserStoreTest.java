/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
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
		assertFalse(UserStore.exists("TESTUSER"));
		UserStore.add(UserConfig.newBuilder().setUsername("TESTUSER").setPassword("abc1234c"));
		assertTrue(UserStore.exists("TESTUSER"));
		UserStore.remove("TESTUSER");
		assertFalse(UserStore.exists("TESTUSER"));
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
