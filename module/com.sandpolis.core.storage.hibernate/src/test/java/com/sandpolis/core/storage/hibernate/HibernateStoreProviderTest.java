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
package com.sandpolis.core.storage.hibernate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.cfg.Configuration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sandpolis.core.instance.storage.StoreProvider;

import net.jodah.concurrentunit.Waiter;

class HibernateStoreProviderTest {

	@Entity
	static class TestObject {

		@Id
		@Column
		private long id;

		@Column
		private String name;

		public TestObject() {
		}

		public TestObject(long id, String name) {
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
		public boolean equals(Object obj) {
			if (obj instanceof TestObject) {
				TestObject o = (TestObject) obj;
				return o.id == this.id && o.name.equals(this.name);
			}

			return false;
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
	void testAdd(HibernateStoreProvider<TestObject> provider) {
		provider.add(o1);
		provider.add(o2);
		provider.add(o3);
		provider.add(o4);
		provider.add(o5);
		provider.add(o6);
	}

	@ParameterizedTest
	@MethodSource("implementations")
	void testGetId(HibernateStoreProvider<TestObject> provider) {
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
	void testGetByField(HibernateStoreProvider<TestObject> provider) {
		provider.add(o1);
		provider.add(o2);
		provider.add(o3);
		provider.add(o4);
		provider.add(o5);
		provider.add(o6);

		assertEquals(o1, provider.get("name", "One").get());
		assertEquals(o2, provider.get("name", "Two").get());

		// Repeat
		assertEquals(o1, provider.get("name", "One").get());
		assertEquals(o2, provider.get("name", "Two").get());
	}

	@ParameterizedTest
	@MethodSource("implementations")
	void testRemove(HibernateStoreProvider<TestObject> provider) {
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
	void testExistsById(HibernateStoreProvider<TestObject> provider) {
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
	void testExistsByField(HibernateStoreProvider<TestObject> provider) {
		provider.add(o1);
		provider.add(o2);
		provider.add(o3);
		provider.add(o4);
		provider.add(o5);
		provider.add(o6);

		provider.remove(o2);
		provider.remove(o4);
		provider.remove(o6);

		assertFalse(provider.exists("name", "Two"));
		assertFalse(provider.exists("name", "Four"));
		assertFalse(provider.exists("name", "Six"));

		assertTrue(provider.exists("name", "One"));
		assertTrue(provider.exists("name", "Three"));
		assertTrue(provider.exists("name", "Five"));
	}

	@ParameterizedTest
	@MethodSource("implementations")
	void testCount(HibernateStoreProvider<TestObject> provider) {
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
	void testStream(HibernateStoreProvider<TestObject> provider) {
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

	static Stream<StoreProvider<TestObject>> implementations() throws Exception {
		Configuration conf = new Configuration()
				// Set the H2 database driver
				.setProperty("hibernate.connection.driver_class", "org.h2.Driver")

				// Set the H2 dialect
				.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")

				// Set the credentials
				.setProperty("hibernate.connection.username", "").setProperty("hibernate.connection.password", "")

				// Set the database URL
				.setProperty("hibernate.connection.url", "jdbc:h2:mem:junit")

				// Set additional options
				.setProperty("hibernate.connection.shutdown", "true")
				.setProperty("hibernate.globally_quoted_identifiers", "true")
				.setProperty("hibernate.hbm2ddl.auto", "create");

		List.of(TestObject.class).forEach(conf::addAnnotatedClass);

		return Stream.of(new HibernateConnection(conf.buildSessionFactory()).provider(TestObject.class, "id"));
	}

}
