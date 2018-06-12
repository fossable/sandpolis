/******************************************************************************
 *                                                                            *
 *                    Copyright 2016 Subterranean Security                    *
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
package com.sandpolis.core.util;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.Test;

public class RandUtilTest {

	@Test
	public void testNextInt() {
		int min = -2;
		int max = 10;

		for (int i = 0; i < 100000; i++) {
			int rand = RandUtil.nextInt(min, max);
			assertTrue(rand >= min);
			assertTrue(rand <= max);
		}

		min = -1;
		max = 0;

		for (int i = 0; i < 100000; i++) {
			int rand = RandUtil.nextInt(min, max);
			assertTrue(rand >= min);
			assertTrue(rand <= max);
		}

	}

	@Test
	public void testNextLong() {
		long min = -2;
		long max = 10;

		for (int i = 0; i < 100000; i++) {
			long rand = RandUtil.nextLong(min, max);
			assertTrue(rand >= min);
			assertTrue(rand <= max);
		}

		min = -1;
		max = 0;

		for (int i = 0; i < 100000; i++) {
			long rand = RandUtil.nextLong(min, max);
			assertTrue(rand >= min);
			assertTrue(rand <= max);
		}

	}

	@Test
	public void testNextAlphabetic() {
		String alpha = RandUtil.nextAlphabetic(10000);
		for (int i = 0; i < alpha.length(); i++) {
			assertTrue(Character.isLetter(alpha.charAt(i)));
		}
	}

	@Test
	public void testNextNumeric() {
		new BigInteger(RandUtil.nextNumeric(10000));
	}

}
