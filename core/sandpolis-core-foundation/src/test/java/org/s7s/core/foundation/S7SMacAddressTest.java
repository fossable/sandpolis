//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S7SMacAddressTest {

	@Test
	@DisplayName("Create valid MAC addresses from strings")
	void createMacAddressesFromString() {
		assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, S7SMacAddress.of("01:02:03:04:05:06").bytes());
		assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0 }, S7SMacAddress.of("00:00:00:00:00:00").bytes());
		assertArrayEquals(new byte[] { -1, -1, -1, -1, -1, -1 }, S7SMacAddress.of("FF:FF:FF:FF:FF:FF").bytes());
	}

	@Test
	@DisplayName("Create invalid MAC addresses from strings")
	void createMacAddressesFromStringInvalid() {
		assertThrows(NullPointerException.class, () -> S7SMacAddress.of((String) null));
		assertThrows(IllegalArgumentException.class, () -> S7SMacAddress.of("00:00:00::00:00:00"));
		assertThrows(IllegalArgumentException.class, () -> S7SMacAddress.of("00:00:00;00:00:00"));
		assertThrows(IllegalArgumentException.class, () -> S7SMacAddress.of("00:00:00:00:00:00:00"));
		assertThrows(IllegalArgumentException.class, () -> S7SMacAddress.of(""));
		assertThrows(IllegalArgumentException.class, () -> S7SMacAddress.of("0i:33:44:55:66:77"));
	}

	@Test
	@DisplayName("Create valid MAC addresses from bytes")
	void createMacAddressesFromBytes() {
		assertEquals("00:00:00:00:00:00", S7SMacAddress.of(new byte[] { 0, 0, 0, 0, 0, 0 }).string());
		assertEquals("FF:FF:FF:FF:FF:FF",
				S7SMacAddress.of(new byte[] { -1, -1, -1, -1, -1, -1 }).string().toUpperCase());
	}
}
