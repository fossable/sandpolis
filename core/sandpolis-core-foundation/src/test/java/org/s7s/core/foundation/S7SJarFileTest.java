//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S7SJarFileTest {

	@Test
	@DisplayName("Get a manifest value that exists")
	void getManifestValue() throws IOException {
		assertEquals("93834",
				S7SJarFile.of("src/test/resources/small_program.jar").getManifestValue("test-attribute").get());
	}

	@Test
	@DisplayName("Get a manifest value that does not exist")
	void getManifestValueNonexist() throws IOException {
		assertTrue(
				S7SJarFile.of("src/test/resources/small_program.jar").getManifestValue("nonexist-attribute").isEmpty());
		assertTrue(S7SJarFile.of("src/test/resources/small_program.jar").getManifestValue("").isEmpty());
	}

	@Test
	@DisplayName("Try to get a value from a nonexistent manifest")
	void getManifestValueWithoutManifest() {
		assertThrows(IOException.class,
				() -> S7SJarFile.of("src/test/resources/test6.jar").getManifestValue("test-attribute"));
	}

	@Test
	@DisplayName("Try to get a value from an invalid file")
	void getManifestValueInvalidFile() {
		assertThrows(IOException.class,
				() -> S7SJarFile.of("src/test/resources/text1.txt").getManifestValue("test-attribute"));
	}

	@Test
	@DisplayName("Get size of jar resources")
	void getResourceSize() throws IOException {
		assertEquals(88,
				S7SJarFile.of("src/test/resources/small_program.jar").getResourceSize("/META-INF/MANIFEST.MF"));
		assertEquals(88, S7SJarFile.of("src/test/resources/small_program.jar").getResourceSize("META-INF/MANIFEST.MF"));
	}

	@Test
	@DisplayName("Try to get the size of nonexistent resources")
	void getResourceSizeNonexist() {
		assertThrows(IOException.class,
				() -> S7SJarFile.of("src/test/resources/small_program.jar").getResourceSize("META-INF/MANIFEST.MF2"));
		assertThrows(IOException.class,
				() -> S7SJarFile.of("src/test/resources/small_program.jar").getResourceSize(""));
	}

	@Test
	@DisplayName("Try to get the size of resources in a nonexistent file")
	void getResourceSizeNonexistFile() {
		assertThrows(IOException.class,
				() -> S7SJarFile.of("src/test/resources/nonexist.jar").getResourceSize("META-INF/MANIFEST.MF"));
	}

	@Test
	@DisplayName("Try to get the size of resources from an invalid file type")
	void getResourceSizeInvalidFile() {
		assertThrows(IOException.class,
				() -> S7SJarFile.of("src/test/resources/small_file.txt").getResourceSize("META-INF/MANIFEST.MF"));
		assertThrows(IOException.class,
				() -> S7SJarFile.of("src/test/resources").getResourceSize("META-INF/MANIFEST.MF"));
	}

	@Test
	@DisplayName("Check for resources in a jar file")
	void resourceExists_1() throws IOException {
		assertTrue(S7SJarFile.of("src/test/resources/small_program.jar").resourceExists("/META-INF/MANIFEST.MF"));
		assertTrue(S7SJarFile.of("src/test/resources/small_program.jar").resourceExists("META-INF/MANIFEST.MF"));
		assertFalse(S7SJarFile.of("src/test/resources/small_program.jar").resourceExists("META-INF/MANIFEST.MF2"));
	}

	@Test
	@DisplayName("Try to check for resources in a text file")
	void resourceExistsInvalidFile() {
		assertThrows(IOException.class,
				() -> S7SJarFile.of("src/test/resources/small_file.txt").resourceExists("META-INF/MANIFEST.MF"));
	}

}
