/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.core.instance.store.pref;

import static com.sandpolis.core.instance.store.pref.PrefStore.PrefStore;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PrefStoreTest {

	@BeforeAll
	public static void setup() {
		PrefStore.init(config -> {
			config.prefNodeClass = PrefStoreTest.class;
		});
	}

	@Test
	@DisplayName("Save and retrieve a String")
	void putString_1() {
		String key = "string_test";
		String value = "1234";

		PrefStore.putString(key, value);
		assertEquals(value, PrefStore.getString(key));
	}

	@Test
	@DisplayName("Save and retrieve a boolean")
	void putBoolean_1() {
		String key = "boolean_test";
		boolean value = true;

		PrefStore.putBoolean(key, value);
		assertEquals(value, PrefStore.getBoolean(key));
	}

	@Test
	@DisplayName("Save and retrieve an int")
	void putInt_1() {
		String key = "int_test";
		int value = 1234;

		PrefStore.putInt(key, value);
		assertEquals(value, PrefStore.getInt(key));
	}

	@Test
	@DisplayName("Save and retrieve a byte array")
	void putBytes_1() {
		String key = "byte_test";
		byte[] value = "1234".getBytes();

		PrefStore.putBytes(key, value);
		assertArrayEquals(value, PrefStore.getBytes(key));
	}
}
