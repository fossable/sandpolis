//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

import org.s7s.core.foundation.Platform.OsType;

class S7SProcessTest {

	@Test
	void testCompleteUnix() {
		assumeTrue(S7SSystem.OS_TYPE != OsType.WINDOWS);

		S7SProcess.exec("true").complete((exit, stdout, stderr) -> {
			assertEquals(0, exit);
			assertEquals("", stdout);
			assertEquals("", stderr);
		});

		S7SProcess.exec("false").complete((exit, stdout, stderr) -> {
			assertEquals(1, exit);
			assertEquals("", stdout);
			assertEquals("", stderr);
		});

		S7SProcess.exec("echo", "true").complete((exit, stdout, stderr) -> {
			assertEquals(0, exit);
			assertEquals("true\n", stdout);
			assertEquals("", stderr);
		});
	}

	@Test
	void testCompleteWindows() {
		assumeTrue(S7SSystem.OS_TYPE == OsType.WINDOWS);

		// TODO
	}

}
