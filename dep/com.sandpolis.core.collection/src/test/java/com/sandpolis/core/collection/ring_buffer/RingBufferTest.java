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
package com.sandpolis.core.collection.ring_buffer;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sandpolis.core.collection.ring_buffer.RingBuffer;

public class RingBufferTest {

	private RingBuffer<Long> buffer;
	private int capacity;

	@Before
	public void setup() {
		capacity = 100;
		buffer = new RingBuffer<>(capacity);
	}

	@Test
	public void testGet() {
		for (int i = 0; i < capacity; i++) {
			buffer.add((long) i);
		}

		for (int i = capacity - 1; i >= 0; i--) {
			assertEquals(buffer.get(capacity - i - 1), (Long) (long) i);
		}

		for (int i = 0; i < 3 * capacity; i++) {
			buffer.add((long) 0);
		}

		for (int i = 0; i < capacity; i++) {
			assertEquals(buffer.get(i), (Long) 0L);
		}

		buffer.add((long) 42);
		buffer.add((long) 55);

		assertEquals(buffer.get(0), (Long) 55L);
		assertEquals(buffer.get(1), (Long) 42L);
	}

	@Test
	public void testSize() {
		assertEquals(0, buffer.size());
		buffer.add(1L);
		assertEquals(1, buffer.size());
		for (int i = 0; i < capacity; i++) {
			buffer.add((long) i);
		}
		assertEquals(capacity, buffer.size());
	}

	@Test
	public void testCapacity() {
		assertEquals(capacity, buffer.capacity());
	}

}
