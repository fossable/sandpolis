//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

import org.s7s.core.foundation.Platform.OsType;

class S7SStringTest {

	@Test
	void testInvalidPrivateIPs() {
		assertFalse(S7SString.of("74.192.155.168").isPrivateIPv4());
		assertFalse(S7SString.of("245.3.36.18").isPrivateIPv4());
		assertFalse(S7SString.of("162.113.53.86").isPrivateIPv4());
		assertFalse(S7SString.of("72.155.10.184").isPrivateIPv4());
		assertFalse(S7SString.of("202.69.223.43").isPrivateIPv4());
		assertFalse(S7SString.of("151.250.62.220").isPrivateIPv4());
		assertFalse(S7SString.of("80.101.92.188").isPrivateIPv4());
		assertFalse(S7SString.of("22.194.149.43").isPrivateIPv4());
		assertFalse(S7SString.of("13.118.39.20").isPrivateIPv4());
		assertFalse(S7SString.of("150.140.194.234").isPrivateIPv4());
		assertFalse(S7SString.of("44.82.127.42").isPrivateIPv4());
	}

	@Test
	void testValidPrivateIPs() {
		assertTrue(S7SString.of("192.168.1.1").isPrivateIPv4());
		assertTrue(S7SString.of("192.168.41.184").isPrivateIPv4());
		assertTrue(S7SString.of("192.168.210.208").isPrivateIPv4());
		assertTrue(S7SString.of("192.168.44.75").isPrivateIPv4());
		assertTrue(S7SString.of("192.168.129.77").isPrivateIPv4());
		assertTrue(S7SString.of("192.168.29.221").isPrivateIPv4());
		assertTrue(S7SString.of("10.0.0.1").isPrivateIPv4());
		assertTrue(S7SString.of("10.252.166.215").isPrivateIPv4());
		assertTrue(S7SString.of("10.207.85.163").isPrivateIPv4());
		assertTrue(S7SString.of("10.146.201.129").isPrivateIPv4());
		assertTrue(S7SString.of("10.198.177.8").isPrivateIPv4());
		assertTrue(S7SString.of("10.70.198.55").isPrivateIPv4());
	}

	@Test
	void testInvalidPorts() {
		assertFalse(S7SString.of("").isPort());
		assertFalse(S7SString.of("123456789").isPort());
		assertFalse(S7SString.of("4000g").isPort());
		assertFalse(S7SString.of("test").isPort());
		assertFalse(S7SString.of("-5000").isPort());
		assertFalse(S7SString.of("65536").isPort());
	}

	@Test
	void testValidPorts() {
		assertTrue(S7SString.of("0").isPort());
		assertTrue(S7SString.of("80").isPort());
		assertTrue(S7SString.of("8080").isPort());
		assertTrue(S7SString.of("10101").isPort());
		assertTrue(S7SString.of("65535").isPort());
	}

	@Test
	void testValidPaths() {
		assertTrue(S7SString.of("test/.test.txt").isPath());
	}

	@Test
	void testInvalidPaths() {
		assertFalse(S7SString.of(new String(new byte[] { 0 })).isPath());
	}

	@Test
	void testInvalidPathsWindows() {
		assumeTrue(S7SSystem.OS_TYPE == OsType.WINDOWS);

		assertFalse(S7SString.of("test/.test.txt ").isPath());
		assertFalse(S7SString.of("test/.test.txt.*").isPath());
		assertFalse(S7SString.of("test/.test?txt").isPath());
		assertFalse(S7SString.of(" ").isPath());
	}
}
