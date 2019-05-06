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

import static com.sandpolis.core.util.NetUtil.checkPort;
import static com.sandpolis.core.util.NetUtil.download;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

}
