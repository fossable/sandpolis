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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

public class NetUtilTest {

	@Test
	public void testDownload() throws IOException {
		// First download to memory
		assertNotNull(NetUtil.download("https://github.com/Subterranean-Security/Sandpolis/blob/master/.gitignore"));

		// Now download to a file
		File out = Files.createTempFile(null, null).toFile();
		assertEquals(0, out.length());
		NetUtil.download("https://github.com/Subterranean-Security/Sandpolis/blob/master/.gitignore", out);
		assertTrue(out.length() > 0);
		out.delete();
	}

	@Test
	public void testDownloadTooLarge() throws IOException {
		assertThrows(IllegalArgumentException.class,
				() -> NetUtil.download("http://releases.ubuntu.com/18.04/ubuntu-18.04-desktop-amd64.iso"));
	}

	@Test
	public void testCheckPort() {
		assertTrue(NetUtil.checkPort("www.google.com", 80));
		assertFalse(NetUtil.checkPort("www.example.com", 81));
		assertFalse(NetUtil.checkPort("8.8.8.8", 80));
	}

}
