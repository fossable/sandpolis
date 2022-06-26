//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.state.oid;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OidTest {

	@Test
	void testFirst() {
		assertEquals("test", Oid.of("/Test/example").first());
	}

	@Test
	void testIsConcrete() {
		assertTrue(Oid.of("/Test/example").isConcrete());
		assertFalse(Oid.of("/Test()/example").isConcrete());
		assertTrue(Oid.of("/Test(123)/example").isConcrete());
	}

	@Test
	void testLast() {
		assertEquals("example", Oid.of("/Test/example").last());
		assertEquals("example", Oid.of("/Test/A()/example").last());
	}

	@Test
	void testResolve() {
		assertEquals("org.s7s.core.instance:/Test/A(a)/B(b)/C()",
				Oid.of("/Test/A()/B()/C()").resolve("a", "b").toString());
	}

	@Test
	void testResolveLast() {
		assertEquals("org.s7s.core.instance:/Test/A()/B(a)/C(b)",
				Oid.of("/test/A()/B()/C()").resolveLast("a", "b").toString());
	}

	@Test
	void testToString() {
		assertEquals("org.s7s.core.instance:/Test/example", Oid.of("/Test/example").toString());
		assertEquals("org.s7s.core.instance:/Test/example", Oid.of("org.s7s.core.instance:/Test/example").toString());
	}

}
