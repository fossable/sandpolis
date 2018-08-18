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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.io.Files;
import com.google.common.io.MoreFiles;

class ArtifactUtilTest {

	private File temp;

	@BeforeEach
	void setup() {
		temp = Files.createTempDir();
	}

	@AfterEach
	void cleanup() throws IOException {
		MoreFiles.deleteRecursively(temp.toPath());
	}

	@Test
	void testGetArtifactFilename() {
		assertEquals("google.com-1.0.jar", ArtifactUtil.getArtifactFilename("com.google:google.com:1.0"));
		assertEquals("test.jar", ArtifactUtil.getArtifactFilename(":test:"));

		assertThrows(IllegalArgumentException.class, () -> ArtifactUtil.getArtifactFilename(":"));
		assertThrows(IllegalArgumentException.class, () -> ArtifactUtil.getArtifactFilename(""));
		assertThrows(IllegalArgumentException.class, () -> ArtifactUtil.getArtifactFilename("test.jar"));
	}

	@Test
	void testDownloadCaching() throws IOException {
		assertTrue(ArtifactUtil.download(temp, "javax.measure:unit-api:1.0"));
		assertFalse(ArtifactUtil.download(temp, "javax.measure:unit-api:1.0"));
	}

	@Test
	void testDownloadCacheCorrupted() throws IOException {
		File dir = Files.createTempDir();
		assertTrue(ArtifactUtil.download(dir, "javax.measure:unit-api:1.0"));

		// Corrupt the downloaded file
		try (PrintWriter pw = new PrintWriter(dir.getAbsolutePath() + "/unit-api-1.0.jar")) {
			pw.print("a");
		}

		assertTrue(ArtifactUtil.download(dir, "javax.measure:unit-api:1.0"));
	}

}
