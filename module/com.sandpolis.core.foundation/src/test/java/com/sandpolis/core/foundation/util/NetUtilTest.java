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

import static com.sandpolis.core.foundation.util.NetUtil.checkPort;
import static com.sandpolis.core.foundation.util.NetUtil.download;
import static com.sandpolis.core.foundation.util.NetUtil.serviceName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NetUtilTest {

	@Test
	@DisplayName("Download a small file into memory")
	void download_1() throws IOException {
		byte[] file = download("https://github.com/Subterranean-Security/Sandpolis/blob/master/.gitignore");

		assertNotNull(file);
		assertTrue(file.length > 0);
	}

	@Test
	@DisplayName("Download a small file to the filesystem")
	void download_2(@TempDir Path temp) throws IOException {
		download("https://github.com/Subterranean-Security/Sandpolis/blob/master/.gitignore",
				temp.resolve("test.txt").toFile());

		assertTrue(Files.exists(temp.resolve("test.txt")));
		assertTrue(Files.size(temp.resolve("test.txt")) > 0);
	}

	@Test
	@DisplayName("Try to download a file that is too big for this method")
	void download_3() throws IOException {
		assertThrows(IllegalArgumentException.class,
				() -> download("http://old-releases.ubuntu.com/releases/11.04/ubuntu-11.04-desktop-amd64.iso"));
	}

	@Test
	@DisplayName("Check some localhost ports")
	void checkPort_1() throws IOException {
		assertFalse(checkPort("127.0.0.1", 8923));

		try (ServerSocket socket = new ServerSocket(8923)) {
			assertTrue(checkPort("127.0.0.1", 8923));
		}

		assertFalse(checkPort("127.0.0.1", 8923));
	}

	@Test
	@DisplayName("Check well-known service names")
	void serviceName_1() {
		assumeTrue(System.getProperty("os.name").toLowerCase().contains("linux"));

		assertEquals("ssh", serviceName(22).get());
		assertEquals("sandpolis", serviceName(8768).get());
	}

}
