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
package com.sandpolis.server.store.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.storage.StoreProviderFactory;
import com.sandpolis.core.proto.pojo.User.UserConfig;
import com.sandpolis.core.proto.util.Result.Outcome;

public class UserStoreTest {

	@BeforeEach
	public void setup() throws URISyntaxException {
		UserStore.init(StoreProviderFactory.memoryList(User.class));
	}

	@Test
	public void testUserExists() {
		assertFalse(UserStore.exists("TESTUSER"));
		UserStore.add(UserConfig.newBuilder().setUsername("TESTUSER2").setPassword("abc1234c").build());
		assertTrue(UserStore.exists("TESTUSER"));
		UserStore.remove("TESTUSER");
		assertFalse(UserStore.exists("TESTUSER"));
	}

	@Test
	public void testLogin() {
		UserStore.add(UserConfig.newBuilder().setUsername("TESTUSER2").setPassword("abc1234c").build());
		assertFalse(UserStore.validLogin("TESTUSER2", "wrongpass").getResult());
		assertFalse(UserStore.validLogin("TESTUSER3", "abc1234c").getResult());
		assertTrue(UserStore.validLogin("TESTUSER2", "abc1234c").getResult());
	}

	@Test
	public void testLoginExpired() {
		UserStore.add(UserConfig.newBuilder().setUsername("TESTUSER2").setPassword("abc1234c").build());
		assertFalse(UserStore.validLogin("TESTUSER2", "abc1234c").getResult());
		UserStore.get("TESTUSER2").get().setExpiration(System.currentTimeMillis() + 10000);
		assertTrue(UserStore.validLogin("TESTUSER2", "abc1234c").getResult());
	}

	@Test
	public void testAddGetUser() {
		Outcome outcome = UserStore
				.add(UserConfig.newBuilder().setUsername("TESTUSER2").setPassword("abc1234c").build());
		assertTrue(outcome.getResult());

		User user = UserStore.get("TESTUSER3").get();
		assertEquals(0, user.getExpiration());
		assertEquals("TESTUSER3", user.getUsername());
		assertNotEquals(0, user.getCreation());
	}

}
