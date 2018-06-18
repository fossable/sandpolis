/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.core.instance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ConstantTreeTest {

	private static Set<Object> tags;

	/**
	 * Test the structure of the tree.
	 */
	public static void testTree(Class<?> c) throws IllegalArgumentException, IllegalAccessException {
		tags = new HashSet<>();
		checkUnique(c);
	}

	/**
	 * Check that all keys are unique in value.
	 */
	private static void checkUnique(Class<?> c) throws IllegalArgumentException, IllegalAccessException {
		for (Class<?> cls : c.getDeclaredClasses()) {
			checkUnique(cls);
		}

		for (Field field : c.getDeclaredFields()) {
			Object tag = field.get(null);
			if (tags.contains(tag)) {
				fail("Found duplicate tag: " + tag + " field: " + field.getName());
			}
			tags.add(tag);
		}
	}

	/**
	 * A test class for {@link testGetFieldString()}.
	 */
	private class TestClass extends ConstantTree<String> {
		public static final String key = "key";

		public final class test {
			public final class test2 {
				public static final String key = "test.test2.key";

				public final class test3 {
					public static final String key = "test.test2.test3.key";
				}
			}
		}
	}

	@Test
	public void testGetFieldString() throws NoSuchFieldException, SecurityException {
		assertEquals(TestClass.test.test2.test3.key,
				ConstantTree.getFieldString(TestClass.test.test2.test3.class.getField("key")));
		assertEquals(TestClass.test.test2.key, ConstantTree.getFieldString(TestClass.test.test2.class.getField("key")));
		assertEquals(TestClass.key, ConstantTree.getFieldString(TestClass.class.getField("key")));
	}

}
