//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.pref;

import static org.s7s.core.instance.pref.PrefStore.PrefStore;
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
