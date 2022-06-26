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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S7SPasswordTest {

	@Test
	@DisplayName("Check PBKDF2 verification")
	void checkPBKDF2() {
		assertTrue(S7SPassword.of("pa55w0rd").checkPBKDF2(
				"2142:7D06806F24653DD5364C6BCFFEC86029:ts3IyK5ws7GI69Nti24WBd5zvOmunZ7eWj/GDV25j09SkmUPl+9HmSw0OXlH5mFq"));
		assertFalse(S7SPassword.of("pa55w0rd").checkPBKDF2(
				"2142:7D06806F24653DD5364C6BCFFEC86029:UQWvabjl1dSWq21Edl+ME7lUb/L9KSKT90K2U6iPCtUGUbNiDnj5TdnGc6irJJgE"));

		assertTrue(S7SPassword.of("pa55w0rd").checkPBKDF2(S7SPassword.of("pa55w0rd").hashPBKDF2()));
	}
}
