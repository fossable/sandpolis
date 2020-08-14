//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.core.foundation.util;

import static com.sandpolis.core.foundation.util.JarUtil.getManifestValue;
import static com.sandpolis.core.foundation.util.JarUtil.getResourceSize;
import static com.sandpolis.core.foundation.util.JarUtil.resourceExists;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JarUtilTest {

	@Test
	@DisplayName("Get a manifest value that exists")
	void getManifestValue_1() throws IOException {
		assertEquals("93834", getManifestValue(new File("src/test/resources/small_program.jar"), "test-attribute").get());
	}

	@Test
	@DisplayName("Get a manifest value that does not exist")
	void getManifestValue_2() throws IOException {
		assertTrue(getManifestValue(new File("src/test/resources/small_program.jar"), "nonexist-attribute").isEmpty());
		assertTrue(getManifestValue(new File("src/test/resources/small_program.jar"), "").isEmpty());
	}

	@Test
	@DisplayName("Try to get a value from a nonexistent manifest")
	void getManifestValue_3() {
		assertThrows(IOException.class,
				() -> getManifestValue(new File("src/test/resources/test6.jar"), "test-attribute"));
	}

	@Test
	@DisplayName("Try to get a value from an invalid file")
	void getManifestValue_4() {
		assertThrows(IOException.class,
				() -> getManifestValue(new File("src/test/resources/text1.txt"), "test-attribute"));
	}

	@Test
	@DisplayName("Get size of jar resources")
	void getResourceSize_1() throws IOException {
		assertEquals(88, getResourceSize(new File("src/test/resources/small_program.jar"), "/META-INF/MANIFEST.MF"));
		assertEquals(88, getResourceSize(new File("src/test/resources/small_program.jar"), "META-INF/MANIFEST.MF"));
	}

	@Test
	@DisplayName("Try to get the size of nonexistent resources")
	void getResourceSize_2() {
		assertThrows(IOException.class,
				() -> getResourceSize(new File("src/test/resources/small_program.jar"), "META-INF/MANIFEST.MF2"));
		assertThrows(IOException.class, () -> getResourceSize(new File("src/test/resources/small_program.jar"), ""));
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
				() -> getResourceSize(new File("src/test/resources/small_file.txt"), "META-INF/MANIFEST.MF"));
		assertThrows(IOException.class, () -> getResourceSize(new File("src/test/resources"), "META-INF/MANIFEST.MF"));
	}

	@Test
	@DisplayName("Check for resources in a jar file")
	void resourceExists_1() throws IOException {
		assertTrue(resourceExists(Paths.get("src/test/resources/small_program.jar"), "/META-INF/MANIFEST.MF"));
		assertTrue(resourceExists(Paths.get("src/test/resources/small_program.jar"), "META-INF/MANIFEST.MF"));
		assertFalse(resourceExists(Paths.get("src/test/resources/small_program.jar"), "META-INF/MANIFEST.MF2"));
	}

	@Test
	@DisplayName("Try to check for resources in a text file")
	void resourceExists_2() {
		assertThrows(IOException.class,
				() -> resourceExists(Paths.get("src/test/resources/small_file.txt"), "META-INF/MANIFEST.MF"));
	}

}
