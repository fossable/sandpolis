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

class S7SIPAddressTest {

	@Test
	@DisplayName("Create valid IPv4 addresses from strings")
	void createIPv4AddressesFromString() {
		assertArrayEquals(new byte[] { 127, 0, 0, 1 }, S7SIPAddress.of("127.0.0.1").asBytes());
		assertArrayEquals(new byte[] { 0, 0, 0, 0 }, S7SIPAddress.of("0.0.0.0").asBytes());
	}

	@Test
	@DisplayName("Create invalid IPv4 addresses from strings")
	void createIPv4AddressesFromStringInvalid() {
		assertThrows(NullPointerException.class, () -> S7SIPAddress.of((String) null));
		assertThrows(IllegalArgumentException.class, () -> S7SIPAddress.of("127.0.0."));
		assertThrows(IllegalArgumentException.class, () -> S7SIPAddress.of("127.0.0.1.1"));
		assertThrows(IllegalArgumentException.class, () -> S7SIPAddress.of(""));
		assertThrows(IllegalArgumentException.class, () -> S7SIPAddress.of("127.0.0.a"));
	}

	@Test
	@DisplayName("Create valid IPv4 addresses from integers")
	void createIPv4AddressesFromInt() {
		assertEquals("0.0.0.0", S7SIPAddress.of(0).asString());
		assertEquals("0.0.0.12", S7SIPAddress.of(12).asString());
	}

	@Test
	@DisplayName("Compute network boundary addresses")
	void computeNetworkBoundaries() {
		assertEquals("192.168.1.1", S7SIPAddress.of("192.168.1.7").getFirstAddressInNetwork(24).asString());
		assertEquals("192.168.1.254", S7SIPAddress.of("192.168.1.7").getLastAddressInNetwork(24).asString());

		assertEquals("192.168.1.1", S7SIPAddress.of("192.168.1.1").getFirstAddressInNetwork(24).asString());
		assertEquals("192.168.1.254", S7SIPAddress.of("192.168.1.254").getLastAddressInNetwork(24).asString());

		assertEquals("10.0.0.1", S7SIPAddress.of("10.1.2.3").getFirstAddressInNetwork(8).asString());
		assertEquals("10.255.255.254", S7SIPAddress.of("10.1.2.3").getLastAddressInNetwork(8).asString());
	}
}
