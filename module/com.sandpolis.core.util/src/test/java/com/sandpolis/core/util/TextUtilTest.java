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
package com.sandpolis.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TextUtilTest {

	@Test
	@DisplayName("Format byte counts")
	void formatByteCount_1() {

		assertEquals("0 B", formatByteCount(0));
		assertEquals("27 B", formatByteCount(27));
		assertEquals("999 B", formatByteCount(999));
		assertEquals("1000 B", formatByteCount(1000));
		assertEquals("1023 B", formatByteCount(1023));
		assertEquals("1.0 KiB", formatByteCount(1024));
		assertEquals("1.7 KiB", formatByteCount(1728));
		assertEquals("108.0 KiB", formatByteCount(110592));
		assertEquals("6.8 MiB", formatByteCount(7077888));
		assertEquals("432.0 MiB", formatByteCount(452984832));
		assertEquals("27.0 GiB", formatByteCount(28991029248));
		assertEquals("1.7 TiB", formatByteCount(1855425871872));
		assertEquals("8.0 EiB", formatByteCount(9223372036854775807));
	}

	@Test
	@DisplayName("Format byte counts in SI system")
	void formatByteCountSI_1() {

		assertEquals("0 B", formatByteCountSI(0));
		assertEquals("27 B", formatByteCountSI(27));
		assertEquals("999 B", formatByteCountSI(999));
		assertEquals("1.0 kB", formatByteCountSI(1000));
		assertEquals("1.0 kB", formatByteCountSI(1023));
		assertEquals("1.0 kB", formatByteCountSI(1024));
		assertEquals("1.7 kB", formatByteCountSI(1728));
		assertEquals("110.6 kB", formatByteCountSI(110592));
		assertEquals("7.1 MB", formatByteCountSI(7077888));
		assertEquals("453.0 MB", formatByteCountSI(452984832));
		assertEquals("29.0 GB", formatByteCountSI(28991029248));
		assertEquals("1.9 TB", formatByteCountSI(1855425871872));
		assertEquals("9.2 EB", formatByteCountSI(9223372036854775807));
	}
}
