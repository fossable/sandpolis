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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class NetUtilTest {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void testDownload() throws IOException {
		// First download to memory
		assertNotNull(NetUtil.download("https://github.com/Subterranean-Security/Sandpolis/blob/master/.gitignore"));

		// Now download to a file
		File out = temp.newFile();
		assertEquals(0, out.length());
		NetUtil.download("https://github.com/Subterranean-Security/Sandpolis/blob/master/.gitignore", out);
		assertTrue(out.length() > 0);

	}

	@Test(expected = IllegalArgumentException.class)
	public void testDownloadTooLarge() throws IOException {
		NetUtil.download("http://releases.ubuntu.com/18.04/ubuntu-18.04-desktop-amd64.iso");
	}

	@Test
	public void testCheckPort() {
		assertTrue(NetUtil.checkPort("www.google.com", 80));
		assertFalse(NetUtil.checkPort("www.example.com", 81));
		assertFalse(NetUtil.checkPort("8.8.8.8", 80));
	}

}
