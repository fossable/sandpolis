//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S7SVersionTest {

	@Test
	@DisplayName("Parse Java version text")
	void testParseJavaVersion() {

		assertEquals("14.0.2", S7SVersion.fromJavaVersionText("""
				openjdk 14.0.2 2020-07-14
				OpenJDK Runtime Environment (build 14.0.2+12)
				OpenJDK 64-Bit Server VM (build 14.0.2+12, mixed mode)
				""").version());
	}

	@Test
	@DisplayName("Compare version strings")
	void testCompareVersion() {

		assertEquals(0, S7SVersion.of("1.0.0").compareTo(S7SVersion.of("1.0.0")));
		assertTrue(S7SVersion.of("1.0.0").compareTo(S7SVersion.of("1.0.1")) < 0);
		assertTrue(S7SVersion.of("1.0.1").compareTo(S7SVersion.of("1.0.0")) > 0);
	}

	@Test
	void testInvalidVersions() {
		assertFalse(S7SVersion.of("5..0").isS7SModuleVersion());
		assertFalse(S7SVersion.of("5..0.0").isS7SModuleVersion());
		assertFalse(S7SVersion.of("5.0.0.0").isS7SModuleVersion());
	}

	@Test
	void testValidVersions() {
		assertTrue(S7SVersion.of("5.0.0").isS7SModuleVersion());
		assertTrue(S7SVersion.of("05.00.00").isS7SModuleVersion());
	}

}
