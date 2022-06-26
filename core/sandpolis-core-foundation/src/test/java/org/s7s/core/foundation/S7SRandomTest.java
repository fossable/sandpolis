//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S7SRandomTest {

	@Test
	@DisplayName("Test alphabetic string generation")
	void nextAlphabetic() {
		assertEquals("", S7SRandom.nextAlphabetic(0));

		for (char c : S7SRandom.nextAlphabetic(1000).toCharArray()) {
			assertTrue(Character.isLetter(c));
		}
	}

	@Test
	@DisplayName("Test numeric string generation")
	void nextNumeric() {
		assertEquals("", S7SRandom.nextNumeric(0));
		assertDoesNotThrow(() -> new BigInteger(S7SRandom.nextNumeric(1000)));
	}

}
