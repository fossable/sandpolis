//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.core.foundation.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CryptoUtilTest {

	@Test
	@DisplayName("Check PBKDF2 verification")
	void pbkdf2_1() {
		assertTrue(CryptoUtil.PBKDF2.check("pa55w0rd",
				"2142:7D06806F24653DD5364C6BCFFEC86029:4cCVyYx+Re7OIE2fUcSpaV/OqpuV9/7XFae/xLEDSmZEILo6lDMV8IzaZFdcqfSR"));
		assertFalse(CryptoUtil.PBKDF2.check("pa55w0rd",
				"2142:7D06806F24653DD5364C6BCFFEC86029:UQWvabjl1dSWq21Edl+ME7lUb/L9KSKT90K2U6iPCtUGUbNiDnj5TdnGc6irJJgE"));

		assertTrue(CryptoUtil.PBKDF2.check("goodpass", CryptoUtil.PBKDF2.hash("goodpass")));
	}
}
