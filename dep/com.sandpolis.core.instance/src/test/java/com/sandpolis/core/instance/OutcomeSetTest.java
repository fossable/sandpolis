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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sandpolis.core.proto.util.Misc.Outcome;

public class OutcomeSetTest {

	@Test
	public void basicUsage() {
		OutcomeSet set = new OutcomeSet();
		set.add(Outcome.newBuilder().setResult(true).setComment("No comment").setTime(1023).setAction("Test1"));
		set.add(Outcome.newBuilder().setResult(true).setComment("Test2"));
		set.add(Outcome.newBuilder().setResult(true));

		assertTrue(set.getResult());
		assertNull(set.getOneFailed());
		assertTrue(set.getFailed().size() == 0);

		set.add(Outcome.newBuilder().setResult(false).setComment("Failed for some unknown reason :)"));

		assertFalse(set.getResult());
		assertNotNull(set.getOneFailed());
		assertTrue(set.getFailed().size() == 1);
		assertEquals("Failed for some unknown reason :)", set.getOneFailed().getComment());
	}

}
