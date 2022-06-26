//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.s7s.core.foundation.Platform.OsType;

class S7SFileTest {

	@Test
	void testWhichLinux() {
		assumeTrue(S7SSystem.OS_TYPE == OsType.LINUX);
		assertTrue(S7SFile.which("id").isPresent());
		assertFalse(S7SFile.which("id123").isPresent());
	}

	@Test
	void testWhichMacos() {
		assumeTrue(S7SSystem.OS_TYPE == OsType.MACOS);
		assertTrue(S7SFile.which("id").isPresent());
		assertFalse(S7SFile.which("id123").isPresent());
	}

	@Test
	void testWhichWindows() {
		assumeTrue(S7SSystem.OS_TYPE == OsType.WINDOWS);
		assertTrue(S7SFile.which("explorer.exe").isPresent());
		assertFalse(S7SFile.which("123456").isPresent());
	}

	@Test
	void testDownload(@TempDir Path temp) throws IOException {
		S7SFile.of(temp.resolve(".gitignore"))
				.download("https://raw.githubusercontent.com/sandpolis/sandpolis/master/.gitignore");
	}

	@Test
	void testOverwrite(@TempDir Path temp) throws IOException {
		var file = temp.resolve("test.txt");
		Files.writeString(file, "1234");
		S7SFile.of(file).overwrite();

		assertArrayEquals(new byte[] { 0, 0, 0, 0 }, Files.readAllBytes(file));
	}

}
