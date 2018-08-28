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
package com.sandpolis.core.instance.store.pref;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PrefStoreTest {

	@BeforeAll
	public static void setup() {
		PrefStore.load(Preferences.userRoot().node("/com/sandpolis/test"));
	}

	@Test
	public void testString() {
		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < 1000; i++) {
			String tag = "string" + new Random().nextInt();
			String value = "" + System.nanoTime();

			map.put(tag, value);
			PrefStore.putString(tag, value);
		}

		for (String tag : map.keySet()) {
			assertEquals(map.get(tag), PrefStore.getString(tag));
		}
	}

	@Test
	public void testBoolean() {
		Map<String, Boolean> map = new HashMap<>();
		for (int i = 0; i < 1000; i++) {
			String tag = "boolean" + new Random().nextInt();
			boolean value = new Random().nextBoolean();

			map.put(tag, value);
			PrefStore.putBoolean(tag, value);
		}

		for (String tag : map.keySet()) {
			assertEquals(map.get(tag), PrefStore.getBoolean(tag));
		}
	}

	@Test
	public void testInt() {
		Map<String, Integer> map = new HashMap<>();
		for (int i = 0; i < 1000; i++) {
			String tag = "integer" + new Random().nextInt();
			int value = (int) System.nanoTime();

			map.put(tag, value);
			PrefStore.putInt(tag, value);
		}

		for (String tag : map.keySet()) {
			assertTrue(map.get(tag) == PrefStore.getInt(tag));
		}
	}

	@Test
	public void testBytes() {
		Map<String, byte[]> map = new HashMap<>();
		for (int i = 0; i < 1000; i++) {
			String tag = "integer" + new Random().nextInt();
			byte[] value = ("" + System.nanoTime()).getBytes();

			map.put(tag, value);
			PrefStore.putBytes(tag, value);
		}

		for (String tag : map.keySet()) {
			assertArrayEquals(map.get(tag), PrefStore.getBytes(tag));
		}
	}

}
