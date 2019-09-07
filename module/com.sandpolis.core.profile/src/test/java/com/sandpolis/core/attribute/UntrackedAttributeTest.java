/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.core.attribute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.proto.util.Update.AttributeNodeUpdate;
import com.sandpolis.core.proto.util.Update.AttributeUpdate;
import com.sandpolis.core.util.RandUtil;

class UntrackedAttributeTest {

	private UntrackedAttribute<String> attribute;

	@BeforeEach
	void setUp() {
		attribute = new UntrackedAttribute<>();

		// Use the attribute randomly before each test
		for (int i = 0; i < RandUtil.nextInt(1, 10); i++) {
			attribute.set(RandUtil.nextAlphabetic(10));
		}
	}

	@Test
	void testTimestamp() {
		for (int i = 0; i < RandUtil.nextInt(1, 10); i++) {
			long d1 = System.currentTimeMillis();
			attribute.set(RandUtil.nextAlphabetic(10));
			long d2 = System.currentTimeMillis();

			long range = d2 - d1;

			assertTrue(d2 - attribute.getTimestamp() <= range);
		}
	}

	@Test
	void testGetSet() {
		for (int i = 0; i < RandUtil.nextInt(1, 10); i++) {
			String expected = RandUtil.nextAlphabetic(10);
			attribute.set(expected);

			assertEquals(expected, attribute.get());
		}
	}

	@Test
	void testMerge() throws Exception {
		for (int i = 0; i < RandUtil.nextInt(1, 10); i++) {
			AttributeUpdate update = AttributeUpdate.newBuilder().setString(RandUtil.nextAlphabetic(10)).build();
			attribute.merge(AttributeNodeUpdate.newBuilder().addAttributeUpdate(update).build());

			assertEquals(update.getString(), attribute.get());
		}
	}

	@Test
	void testGetUpdates() {
		for (int i = 0; i < RandUtil.nextInt(100, 1000); i++) {
			long d1 = System.currentTimeMillis();

			String expected = RandUtil.nextAlphabetic(10);
			attribute.set(expected);

			assertEquals(expected, attribute.getUpdates(d1).getAttributeUpdate(0).getString());
		}
	}
}
