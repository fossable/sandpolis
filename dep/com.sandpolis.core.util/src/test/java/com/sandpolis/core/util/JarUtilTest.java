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

import static com.sandpolis.core.util.JarUtil.getManifestValue;
import static com.sandpolis.core.util.JarUtil.getResourceSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JarUtilTest {

	@Test
	@DisplayName("Get a manifest value that exists")
	void getManifestValue_1() throws IOException {
		assertEquals("93834", getManifestValue(new File("src/test/resources/test1.jar"), "test-attribute"));
	}

	@Test
	@DisplayName("Get a manifest value that does not exist")
	void getManifestValue_2() throws IOException {
		assertNull(getManifestValue(new File("src/test/resources/test1.jar"), "test-attribute2"));
		assertNull(getManifestValue(new File("src/test/resources/test1.jar"), ""));
	}

	@Test
	@DisplayName("Try to get a value from a nonexistent manifest")
	void getManifestValue_3() {
		assertThrows(IOException.class,
				() -> getManifestValue(new File("src/test/resources/test6.jar"), "test-attribute"));
	}

	@Test
	@DisplayName("Get size of jar resources")
	void getResourceSize_1() throws IOException {
		assertEquals(88, getResourceSize(new File("src/test/resources/test1.jar"), "/META-INF/MANIFEST.MF"));
		assertEquals(88, getResourceSize(new File("src/test/resources/test1.jar"), "META-INF/MANIFEST.MF"));
	}

	@Test
	@DisplayName("Try to get the size of nonexistent resources")
	void getResourceSize_2() {
		assertThrows(IOException.class,
				() -> getResourceSize(new File("src/test/resources/test1.jar"), "META-INF/MANIFEST.MF2"));
		assertThrows(IOException.class, () -> getResourceSize(new File("src/test/resources/test1.jar"), ""));
	}

	@Test
	@DisplayName("Try to get the size of resources in a nonexistent file")
	void getResourceSize_3() {
		assertThrows(IOException.class,
				() -> getResourceSize(new File("src/test/resources/nonexist.jar"), "META-INF/MANIFEST.MF2"));
	}

	@Test
	@DisplayName("Try to get the size of resources from an invalid file type")
	void getResourceSize_4() {
		assertThrows(IOException.class,
				() -> getResourceSize(new File("src/test/resources/test1.txt"), "META-INF/MANIFEST.MF"));
		assertThrows(IOException.class, () -> getResourceSize(new File("src/test/resources"), "META-INF/MANIFEST.MF"));
	}

}
