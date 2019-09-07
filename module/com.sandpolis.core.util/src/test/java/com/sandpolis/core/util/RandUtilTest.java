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
package com.sandpolis.core.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RandUtilTest {

	@Test
	@DisplayName("Test bounded integer generation")
	void nextInt_1() {

		for (int[] range : new int[][] { { -2, 10 }, { -1, 1 }, { 1, 100 }, { -100, -1 }, { 0, 1 } }) {
			int min = range[0];
			int max = range[1];

			for (int i = 0; i < 1000; i++) {
				int rand = RandUtil.nextInt(min, max);
				assertTrue(rand >= min);
				assertTrue(rand <= max);
			}
		}
	}

	@Test
	@DisplayName("Test bounded long generation")
	void nextLong_1() {

		for (long[] range : new long[][] { { -2, 10 }, { -1, 1 }, { 1, 100 }, { -100, -1 }, { 0, 1 } }) {
			long min = range[0];
			long max = range[1];

			for (int i = 0; i < 1000; i++) {
				long rand = RandUtil.nextLong(min, max);
				assertTrue(rand >= min);
				assertTrue(rand <= max);
			}
		}
	}

	@Test
	@DisplayName("Test alphabetic string generation")
	void nextAlphabetic_1() {
		assertEquals("", RandUtil.nextAlphabetic(0));

		for (char c : RandUtil.nextAlphabetic(1000).toCharArray()) {
			assertTrue(Character.isLetter(c));
		}
	}

	@Test
	@DisplayName("Test numeric string generation")
	void nextNumeric_1() {
		assertEquals("", RandUtil.nextNumeric(0));
		assertDoesNotThrow(() -> new BigInteger(RandUtil.nextNumeric(1000)));
	}

}
