package com.sandpolis.core.instance.state.oid;

import static com.sandpolis.core.instance.state.oid.AbsoluteOid.newOid;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OidTest {

	@Test
	@DisplayName("Check whether OIDs are concrete")
	void testIsConcrete() {
		assertTrue(newOid(1).isConcrete());
		assertTrue(newOid(1, 2).isConcrete());
		assertFalse(newOid(0).isConcrete());
		assertFalse(newOid(1, 0).isConcrete());
		assertFalse(newOid(1, 0, 1).isConcrete());
	}

	@Test
	@DisplayName("Check child OIDs")
	void testChild() {
		assertArrayEquals(new long[] { 1, 2 }, newOid(1).child(2).value());
		assertArrayEquals(new long[] { 1, 0 }, newOid(1).child(0).value());
	}

	@Test
	@DisplayName("Check parent OIDs")
	void testParent() {
		assertArrayEquals(new long[] { 1 }, newOid(1, 2).parent().value());
		assertEquals(null, newOid(1).parent());
	}

	@Test
	@DisplayName("Check OID relativization")
	void testRelativize() {
		assertArrayEquals(new long[] { 2 }, newOid(1, 2).relativize(newOid(1)).value());
	}

	@Test
	@DisplayName("Check OID tail")
	void testTail() {
		assertArrayEquals(new long[] { 2 }, newOid(1, 2).tail().value());
		assertThrows(Exception.class, () -> newOid(1).tail());
	}

	@Test
	@DisplayName("Check OID value")
	void testValue() {
		assertArrayEquals(new long[] { 1, 2 }, newOid(1, 2).value());
	}

	@Test
	@DisplayName("Check dotted representation")
	void testToString() {
		assertEquals("1", newOid(1).toString());
		assertEquals("1.2", newOid(1, 2).toString());
	}

	@Test
	@DisplayName("Check compareTo")
	void testCompareTo() {
		assertEquals(-1, newOid(1).compareTo(newOid(2)));
		assertEquals(-1, newOid(0, 0).compareTo(newOid(0, 1)));
		assertEquals(-1, newOid(0, 1).compareTo(newOid(0, 1, 1)));

		assertEquals(0, newOid(1).compareTo(newOid(1)));

		assertEquals(1, newOid(1).compareTo(newOid(0)));
		assertEquals(1, newOid(1, 1).compareTo(newOid(1)));
	}

	@Test
	@DisplayName("Check head")
	void testHead() {
		assertArrayEquals(new long[] { 1 }, newOid(1, 2, 3).head(1).value());
		assertArrayEquals(new long[] { 1, 2 }, newOid(1, 2, 3).head(2).value());
		assertArrayEquals(new long[] { 1, 2, 3 }, newOid(1, 2, 3).head(3).value());
	}

	@Test
	@DisplayName("Check resolve")
	void testResolve() {
		assertArrayEquals(new long[] { 1, 1, 3 }, newOid(1, 0, 3).resolve(1).value());
		assertArrayEquals(new long[] { 1, 1, 3 }, newOid(1, 0, 3).resolve(1, 2).value());
		assertArrayEquals(new long[] { 1, 1, 0 }, newOid(1, 0, 0).resolve(1).value());
	}

	@Test
	@DisplayName("Check OID size")
	void testSize() {
		assertEquals(1, newOid(1).size());
		assertEquals(2, newOid(1, 2).size());
	}

}
