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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class JarUtilTest {

	@Test
	public void testGetManifestValueExists() throws IOException {
		assertEquals("93834", JarUtil.getManifestValue("test-attribute", new File("src/test/resources/test1.jar")));
	}

	@Test
	public void testGetManifestValueNotExists() throws IOException {
		assertNull(JarUtil.getManifestValue("test-attribute2", new File("src/test/resources/test1.jar")));
	}

	@Test(expected = IOException.class)
	public void testGetManifestFileNotExists() throws IOException {
		assertNull(JarUtil.getManifestValue("test-attribute", new File("src/test/resources/test6.jar")));
	}

	@Test
	public void testGetResourceSize() throws IOException {
		assertEquals(88, JarUtil.getResourceSize("META-INF/MANIFEST.MF", new File("src/test/resources/test1.jar")));
	}

}
