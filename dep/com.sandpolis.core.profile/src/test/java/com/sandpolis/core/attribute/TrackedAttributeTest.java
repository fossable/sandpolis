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
package com.sandpolis.core.attribute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.util.RandUtil;

class TrackedAttributeTest {

	private TrackedAttribute<String> attribute;
	private List<String> list;

	@BeforeEach
	public void setUp() {
		attribute = new TrackedAttribute<>();
		list = new ArrayList<>();

		for (int i = 0; i < RandUtil.nextInt(10, 100); i++) {
			String rand = RandUtil.nextAlphabetic(10);
			attribute.set(rand);
			list.add(rand);
		}
	}

	@Test
	void testClear() {
		attribute.clearHistory();
		assertEquals(0, attribute.size());
	}

	@Test
	void testSize() {
		assertEquals(list.size(), attribute.size() + 1);
		attribute.set("new");
		list.add("new");
		assertEquals(list.size(), attribute.size() + 1);
		checkEquality();
	}

	@Test
	void testGetSet() {
		for (int i = 0; i < RandUtil.nextInt(10, 100); i++) {
			String rand = RandUtil.nextAlphabetic(10);
			attribute.set(rand);
			list.add(rand);
			checkEquality();
		}
	}

	private void checkEquality() {
		assertEquals(list.size(), attribute.size() + 1);
		for (int i = 0; i < list.size() - 1; i++) {
			assertEquals(list.get(i), attribute.getValue(i));
		}
		assertEquals(list.get(list.size() - 1), attribute.get());
	}
}
