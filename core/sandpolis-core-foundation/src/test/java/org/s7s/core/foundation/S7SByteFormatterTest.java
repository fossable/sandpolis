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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S7SByteFormatterTest {

	@Test
	@DisplayName("Format byte counts")
	void testFormatByteCount() {

		assertEquals("0 B", S7SByteFormatter.of(0L).humanReadable());
		assertEquals("27 B", S7SByteFormatter.of(27L).humanReadable());
		assertEquals("999 B", S7SByteFormatter.of(999L).humanReadable());
		assertEquals("1000 B", S7SByteFormatter.of(1000L).humanReadable());
		assertEquals("1023 B", S7SByteFormatter.of(1023L).humanReadable());
		assertEquals("1.0 KiB", S7SByteFormatter.of(1024L).humanReadable());
		assertEquals("1.7 KiB", S7SByteFormatter.of(1728L).humanReadable());
		assertEquals("108.0 KiB", S7SByteFormatter.of(110592L).humanReadable());
		assertEquals("6.8 MiB", S7SByteFormatter.of(7077888L).humanReadable());
		assertEquals("432.0 MiB", S7SByteFormatter.of(452984832L).humanReadable());
		assertEquals("27.0 GiB", S7SByteFormatter.of(28991029248L).humanReadable());
		assertEquals("1.7 TiB", S7SByteFormatter.of(1855425871872L).humanReadable());
		assertEquals("8.0 EiB", S7SByteFormatter.of(9223372036854775807L).humanReadable());
	}

	@Test
	@DisplayName("Format byte counts in SI system")
	void testFormatByteCountSI() {

		assertEquals("0 B", S7SByteFormatter.of(0L).humanReadableSI());
		assertEquals("27 B", S7SByteFormatter.of(27L).humanReadableSI());
		assertEquals("999 B", S7SByteFormatter.of(999L).humanReadableSI());
		assertEquals("1.0 kB", S7SByteFormatter.of(1000L).humanReadableSI());
		assertEquals("1.0 kB", S7SByteFormatter.of(1023L).humanReadableSI());
		assertEquals("1.0 kB", S7SByteFormatter.of(1024L).humanReadableSI());
		assertEquals("1.7 kB", S7SByteFormatter.of(1728L).humanReadableSI());
		assertEquals("110.6 kB", S7SByteFormatter.of(110592L).humanReadableSI());
		assertEquals("7.1 MB", S7SByteFormatter.of(7077888L).humanReadableSI());
		assertEquals("453.0 MB", S7SByteFormatter.of(452984832L).humanReadableSI());
		assertEquals("29.0 GB", S7SByteFormatter.of(28991029248L).humanReadableSI());
		assertEquals("1.9 TB", S7SByteFormatter.of(1855425871872L).humanReadableSI());
		assertEquals("9.2 EB", S7SByteFormatter.of(9223372036854775807L).humanReadableSI());
	}
}
