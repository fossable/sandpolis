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
package com.sandpolis.core.instance.store;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import javax.persistence.Id;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sandpolis.core.instance.data.StateObject;
import com.sandpolis.core.instance.store.MemoryListStoreProvider;
import com.sandpolis.core.instance.store.MemoryMapStoreProvider;
import com.sandpolis.core.instance.store.StoreProvider;

import net.jodah.concurrentunit.Waiter;

class StoreProviderTest {

	static class TestObject extends StateObject {

		@Id
		private long id;

		private String name;

		public TestObject(long id, String name) {
			super(null);
			this.id = id;
			this.name = name;
		}

		public long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return id + ": " + name;
		}

		@Override
		public int tag() {
			return 0;
		}
	}

	private TestObject o1 = new TestObject(1L, "One");
	private TestObject o2 = new TestObject(2L, "Two");
	private TestObject o3 = new TestObject(3L, "Three");
	private TestObject o4 = new TestObject(4L, "Four");
	private TestObject o5 = new TestObject(5L, "Five");
	private TestObject o6 = new TestObject(6L, "Six");

	@ParameterizedTest
	@MethodSource("implementations")
	void testAdd(StoreProvider<TestObject> provider) {
		provider.add(o1);
		provider.add(o2);
		provider.add(o3);
		provider.add(o4);
		provider.add(o5);
		provider.add(o6);
	}

	@ParameterizedTest
	@MethodSource("implementations")
	void testGetId(StoreProvider<TestObject> provider) {
		provider.add(o1);
		provider.add(o2);
		provider.add(o3);
		provider.add(o4);
		provider.add(o5);
		provider.add(o6);

		assertEquals(o1, provider.get(1L).get());
		assertEquals(o2, provider.get(2L).get());
		assertEquals(o4, provider.get(4L).get());

		// Repeat
		assertEquals(o1, provider.get(1L).get());
		assertEquals(o2, provider.get(2L).get());
		assertEquals(o4, provider.get(4L).get());
	}

	@ParameterizedTest
	@MethodSource("implementations")
	void testRemove(StoreProvider<TestObject> provider) {
		provider.add(o1);
		provider.add(o2);
		provider.add(o3);
		provider.add(o4);
		provider.add(o5);
		provider.add(o6);

		provider.remove(o2);
		provider.remove(o4);
		provider.remove(o6);

		assertFalse(provider.get(2L).isPresent());
		assertFalse(provider.get(4L).isPresent());
		assertFalse(provider.get(6L).isPresent());

		assertEquals(o1, provider.get(1L).get());
		assertEquals(o3, provider.get(3L).get());
		assertEquals(o5, provider.get(5L).get());
	}

	@ParameterizedTest
	@MethodSource("implementations")
	void testExistsById(StoreProvider<TestObject> provider) {
		provider.add(o1);
		provider.add(o2);
		provider.add(o3);
		provider.add(o4);
		provider.add(o5);
		provider.add(o6);

		provider.remove(o2);
		provider.remove(o4);
		provider.remove(o6);

		assertFalse(provider.exists(2L));
		assertFalse(provider.exists(4L));
		assertFalse(provider.exists(6L));

		assertTrue(provider.exists(1L));
		assertTrue(provider.exists(3L));
		assertTrue(provider.exists(5L));
	}

	@ParameterizedTest
	@MethodSource("implementations")
	void testCount(StoreProvider<TestObject> provider) {
		assertEquals(0, provider.count());
		provider.add(o1);
		assertEquals(1, provider.count());
		provider.add(o2);
		assertEquals(2, provider.count());
		provider.add(o3);
		assertEquals(3, provider.count());
		provider.add(o4);
		assertEquals(4, provider.count());
		provider.add(o5);
		assertEquals(5, provider.count());
		provider.add(o6);
		assertEquals(6, provider.count());
		provider.remove(o2);
		assertEquals(5, provider.count());
	}

	@ParameterizedTest
	@MethodSource("implementations")
	void testStream(StoreProvider<TestObject> provider) {
		provider.add(o1);
		provider.add(o2);
		provider.add(o4);

		try (Stream<TestObject> stream = provider.stream()) {
			assertArrayEquals(new TestObject[] { o1, o2, o4 }, stream.toArray(TestObject[]::new));
		}
	}

	@ParameterizedTest
	@MethodSource("implementations")
	void testConcurrency(StoreProvider<TestObject> provider) throws Exception {
		Set<Thread> threads = new HashSet<>();
		Random rand = new Random();
		Waiter waiter = new Waiter();

		// Add some concurrent mutators
		for (TestObject o : new TestObject[] { o1, o2, o3, o4, o5, o6 })
			threads.add(new Thread(() -> {
				for (int i = 0; i < rand.nextInt(50); i++) {
					try {
						provider.add(o);
						Thread.sleep(rand.nextInt(200));
						provider.remove(o);
					} catch (Throwable e) {
						waiter.fail(e);
					}
				}
				waiter.resume();
			}));

		// Add some get requests
		for (TestObject o : new TestObject[] { o1, o2, o3, o4, o5, o6 })
			threads.add(new Thread(() -> {
				for (int i = 0; i < rand.nextInt(50); i++) {
					try {
						// May be null or nonnull
						provider.get(o.getId());
						Thread.sleep(rand.nextInt(200));
					} catch (Throwable e) {
						waiter.fail(e);
					}
				}
				waiter.resume();
			}));

		// Add some iterators
		for (TestObject o : new TestObject[] { o1, o2, o3, o4, o5, o6 })
			threads.add(new Thread(() -> {
				for (int i = 0; i < rand.nextInt(50); i++) {
					try (Stream<TestObject> stream = provider.stream()) {
						waiter.assertTrue(stream.count() <= 6);
						Thread.sleep(rand.nextInt(200));
					} catch (Throwable e) {
						waiter.fail(e);
					}
				}
				waiter.resume();
			}));

		// Start everything at once
		for (Thread thread : threads)
			thread.start();
		waiter.await(30000, 18);
	}

	static Stream<StoreProvider<TestObject>> implementations() {
		return Stream.of(new MemoryListStoreProvider<Long, TestObject>(TestObject.class),
				new MemoryMapStoreProvider<Long, TestObject>(TestObject.class));
	}

}
