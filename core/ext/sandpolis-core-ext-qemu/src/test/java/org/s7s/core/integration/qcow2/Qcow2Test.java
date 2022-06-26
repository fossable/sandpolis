//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.integration.qcow2;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Qcow2Test {

	@Test
	void testHeader() throws Exception {
		var qcow2 = new Qcow2(Paths.get("src/test/resources/empty_small.qcow2"));

		assertEquals(3, qcow2.header.version());
		assertEquals(0, qcow2.header.nb_snapshots());
		assertEquals(16, qcow2.header.cluster_bits());
		assertEquals(1024, qcow2.header.size());
	}

	@Test
	void testReadSmall() throws Exception {
		var qcow2 = new Qcow2(Paths.get("src/test/resources/small2.qcow2"));

		var buffer = ByteBuffer.allocate(145);
		assertEquals(145, qcow2.read(buffer, 0));
		assertArrayEquals(Files.readAllBytes(Paths.get("src/test/resources/small.txt")), buffer.array());
	}

	@Test
	void testReadSmallInputStream() throws Exception {
		var qcow2 = new Qcow2(Paths.get("src/test/resources/small2.qcow2"));

		try (var out = new ByteArrayOutputStream()) {
			qcow2.newInputStream().transferTo(out);

			assertArrayEquals(Files.readAllBytes(Paths.get("src/test/resources/small.txt")), out.toByteArray());
		}
	}
}
