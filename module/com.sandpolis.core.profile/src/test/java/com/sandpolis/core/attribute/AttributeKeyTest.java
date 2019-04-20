/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class AttributeKeyTest {

	@Test
	void testChains() {
		AttributeDomainKey root = new AttributeDomainKey(null);

		// Create a plural group with a 1-byte plurality
		AttributeGroupKey cpu = new AttributeGroupKey(root, 14, 1);
		AttributeNodeKey model = AttributeKey.newBuilder(cpu, 5).build();
		AttributeGroupKey core = new AttributeGroupKey(cpu, 2, 4);
		AttributeNodeKey temp = AttributeKey.newBuilder(core, 2).build();

		// Create a plural group with a 4-byte plurality
		AttributeGroupKey gpu = new AttributeGroupKey(root, 12, 4);
		AttributeNodeKey vendor = AttributeKey.newBuilder(gpu, 2).build();

		// Check CPU chains
		assertArrayEquals(new byte[] { 14, 0 }, cpu.chain().toByteArray());
		assertArrayEquals(new byte[] { 14, 0, 5 }, model.chain().toByteArray());
		assertArrayEquals(new byte[] { 14, 0, 2, 0, 0, 0, 0 }, core.chain().toByteArray());
		assertArrayEquals(new byte[] { 14, 0, 2, 0, 0, 0, 0, 2 }, temp.chain().toByteArray());

		// Check GPU chains
		assertArrayEquals(new byte[] { 12, 0, 0, 0, 0 }, gpu.chain().toByteArray());
		assertArrayEquals(new byte[] { 12, 0, 0, 0, 0, 2 }, vendor.chain().toByteArray());
	}

}
