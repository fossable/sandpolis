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
package com.sandpolis.core.util;

import static com.sandpolis.core.util.ArtifactUtil.*;
import static com.sandpolis.core.util.ArtifactUtil.ParsedCoordinate.fromCoordinate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArtifactUtilTest {

	@Test
	@DisplayName("Get info from valid coordinates")
	void fromCoordinate_1() {
		assertEquals("a:b:c", fromCoordinate("a:b:c").coordinate);
		assertEquals("a", fromCoordinate("a:b:c").groupId);
		assertEquals("b", fromCoordinate("a:b:c").artifactId);
		assertEquals("c", fromCoordinate("a:b:c").version);
		assertEquals("b-c.jar", fromCoordinate("a:b:c").filename);

		assertEquals(":test:", fromCoordinate(":test:").coordinate);
		assertEquals("", fromCoordinate(":test:").groupId);
		assertEquals("test", fromCoordinate(":test:").artifactId);
		assertEquals("", fromCoordinate(":test:").version);
		assertEquals("test.jar", fromCoordinate(":test:").filename);
	}

	@Test
	@DisplayName("Try to get info from invalid coordinates")
	void fromCoordinate_2() {
		assertThrows(NullPointerException.class, () -> fromCoordinate(null));
		assertThrows(IllegalArgumentException.class, () -> fromCoordinate(""));
		assertThrows(IllegalArgumentException.class, () -> fromCoordinate(":"));
		assertThrows(IllegalArgumentException.class, () -> fromCoordinate("test.jar"));
	}

	@Test
	@DisplayName("Check download")
	void download_1(@TempDir Path temp) throws IOException {
		download(temp, "javax.measure:unit-api:1.0");
		assertTrue(Files.exists(temp.resolve("unit-api-1.0.jar")));
	}

	@Test
	@DisplayName("Check an artifact's hash")
	void checkHash_1(@TempDir Path temp) throws IOException {
		download(temp, "javax.measure:unit-api:1.0");
		assertTrue(checkHash(temp, "javax.measure:unit-api:1.0"));

		// Intentionally corrupt the downloaded file
		try (PrintWriter pw = new PrintWriter(temp.resolve("unit-api-1.0.jar").toFile())) {
			pw.print("a");
		}

		assertFalse(checkHash(temp, "javax.measure:unit-api:1.0"));
	}

	@Test
	@DisplayName("Get the latest version of an existing artifact")
	void getLatestVersion_1() throws IOException {
		// Check an artifact that is unlikely to be updated ever again
		assertEquals("3.6.0.Beta2", getLatestVersion("org.hibernate:hibernate:"));
		assertEquals("3.6.0.Beta2", getLatestVersion("org.hibernate:hibernate:3.5.4-Final"));
	}

	@Test
	@DisplayName("Try to get the latest version of a nonexistent artifact")
	void getLatestVersion_2() {
		// Hopefully no one creates this artifact
		assertThrows(IOException.class, () -> getLatestVersion("1234:5678:"));
	}

}
