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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class S7SMavenArtifactTest {

	@Test
	@DisplayName("Get info from valid coordinates")
	void createArtifacts() {
		assertEquals("a", S7SMavenArtifact.of("a:b:c").groupId());
		assertEquals("b", S7SMavenArtifact.of("a:b:c").artifactId());
		assertEquals("c", S7SMavenArtifact.of("a:b:c").version());
		assertEquals("b-c.jar", S7SMavenArtifact.of("a:b:c").filename());

		assertEquals(null, S7SMavenArtifact.of(":test:").groupId());
		assertEquals("test", S7SMavenArtifact.of(":test:").artifactId());
		assertEquals(null, S7SMavenArtifact.of(":test:").version());
		assertEquals("test.jar", S7SMavenArtifact.of(":test:").filename());
	}

	@Test
	@DisplayName("Try to get info from invalid coordinates")
	void createArtfifactsInvalid() {
		assertThrows(NullPointerException.class, () -> S7SMavenArtifact.of(null));
		assertThrows(IllegalArgumentException.class, () -> S7SMavenArtifact.of(""));
		assertThrows(IllegalArgumentException.class, () -> S7SMavenArtifact.of(":"));
	}

	@Test
	@DisplayName("Check download")
	void downloadArtifact(@TempDir Path temp) throws IOException {
		S7SMavenArtifact.of("javax.measure:unit-api:1.0").download();
	}

	@Test
	@DisplayName("Check an artifact's hash")
	void checkArtifactHash(@TempDir Path temp) throws IOException {

		var artifact = S7SMavenArtifact.of("javax.measure:unit-api:1.0");

		try (var out = new FileOutputStream(temp.resolve("unit-api-1.0.jar").toFile())) {
			artifact.download().transferTo(out);
		}

		assertTrue(artifact.checkHash(temp.resolve("unit-api-1.0.jar")));

		// Intentionally corrupt the downloaded file
		try (PrintWriter pw = new PrintWriter(temp.resolve("unit-api-1.0.jar").toFile())) {
			pw.print("a");
		}

		assertFalse(artifact.checkHash(temp.resolve("unit-api-1.0.jar")));
	}

	@Test
	@DisplayName("Get the latest version of an existing artifact")
	void getLatestVersion() throws IOException {
		// Check an artifact that is unlikely to be updated ever again
		assertEquals("3.6.0.Beta2", S7SMavenArtifact.of("org.hibernate:hibernate:").getLatestVersion());
		assertEquals("3.6.0.Beta2", S7SMavenArtifact.of("org.hibernate:hibernate:3.5.4-Final").getLatestVersion());
	}

	@Test
	@DisplayName("Try to get the latest version of a nonexistent artifact")
	void getLatestVersionNonexist() {
		// Hopefully no one creates this artifact
		assertThrows(IOException.class, () -> S7SMavenArtifact.of("1234:5678:").getLatestVersion());
	}

}
