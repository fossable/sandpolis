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
package com.sandpolis.core.net.loop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class LoopCycleTest {

	@Test
	public void testStatic() {
		LoopCycle cycler = new LoopCycle(1234);
		assertEquals(1234, cycler.nextTimeout());
		assertEquals(1234, cycler.nextTimeout());
		assertEquals(1234, cycler.nextTimeout());
		assertEquals(1234, cycler.getTimeout());
		assertEquals(3, cycler.getIterations());
	}

	@Test
	public void testDynamic() {
		LoopCycle cycler = new LoopCycle(120, 150, 1.0);
		int timeout = cycler.getTimeout();

		while (timeout < 150) {
			assertTrue(timeout < cycler.nextTimeout());
			timeout = cycler.getTimeout();
		}

		for (int i = 0; i < 100; i++)
			assertEquals(150, cycler.nextTimeout());
	}

}
