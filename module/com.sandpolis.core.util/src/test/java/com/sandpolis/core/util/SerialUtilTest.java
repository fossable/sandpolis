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
package com.sandpolis.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class SerialUtilTest {

	@Test
	public void testCorrectUsage() throws ClassNotFoundException, IOException {
		String str = RandUtil.nextAlphabetic(10000);
		assertEquals(str, SerialUtil.deserialize(SerialUtil.serialize(str)));
		assertEquals(str, SerialUtil.deserialize(SerialUtil.serialize(str, true), true));
		assertEquals(str, SerialUtil.deserialize(SerialUtil.serialize(str, false), false));
	}

	@Test
	public void testIncorrectUsage1() {
		String str = RandUtil.nextAlphabetic(10000);
		assertThrows(IOException.class, () -> SerialUtil.deserialize(SerialUtil.serialize(str, false), true));
	}

	@Test
	public void testIncorrectUsage2() {
		String str = RandUtil.nextAlphabetic(10000);
		assertThrows(IOException.class, () -> SerialUtil.deserialize(SerialUtil.serialize(str, true), false));
	}

}
