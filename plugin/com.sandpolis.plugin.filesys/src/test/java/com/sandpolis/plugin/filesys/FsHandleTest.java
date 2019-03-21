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
package com.sandpolis.plugin.filesys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zeroturnaround.zip.ZipUtil;

import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.sandpolis.core.proto.net.MCFsHandle.FileListlet;
import com.sandpolis.core.proto.net.MCFsHandle.FileListlet.UpdateType;

class FsHandleTest {

	private static FsHandle fs;

	private static File temp;

	private static final File test_jar = new File("src/test/resources/test.jar");
	private static final File test_zip = new File("src/test/resources/test.zip");

	@BeforeEach
	void setup() throws IOException {
		temp = Files.createTempDir();

		ZipUtil.unpack(test_jar, temp);
		ZipUtil.unpack(test_zip, temp);

		fs = new FsHandle(temp.getAbsolutePath());
	}

	@AfterEach
	void close() throws IOException {
		fs.close();
		MoreFiles.deleteRecursively(temp.toPath());
	}

	@Test
	void testDown() {
		assertTrue(fs.setPath(temp.getAbsolutePath()));
		assertFalse(fs.down("logback.xml"));
		assertFalse(fs.down("test5"));
		assertTrue(fs.down("test1"));
	}

	@Test
	void testSetPath() {
		assertTrue(fs.setPath(temp.getAbsolutePath() + "/test1/test/test/test/test"));
		assertEquals(temp.getAbsolutePath() + "/test1/test/test/test/test", fs.pwd());
		assertTrue(fs.setPath(temp.getAbsolutePath() + "/test1"));
		assertEquals(temp.getAbsolutePath() + "/test1", fs.pwd());
	}

	@Test
	void testUp() {
		assertTrue(fs.setPath(temp.getAbsolutePath() + "/test1/test/test/test/test"));
		assertTrue(fs.up());
		assertEquals(temp.getAbsolutePath() + "/test1/test/test/test", fs.pwd());
	}

	@Test
	void testUpRoot() {
		assertTrue(fs.setPath("/"));
		assertFalse(fs.up());
	}

	@Test
	void testList() throws IOException {
		assertTrue(fs.setPath(temp.getAbsolutePath()));
		assertEquals(8, fs.list().size());
	}

	@Test
	void testCallbacks() throws Exception {
		List<FileListlet> updates = new ArrayList<>();

		fs.addUpdateCallback(ev -> {
			updates.addAll(ev.getListingList());
		});

		// Add file
		new File(temp.getAbsolutePath() + "/test_create.txt").createNewFile();
		Thread.sleep(300);// Wait for update
		assertEquals(1, updates.size());
		FileListlet fileCreated = updates.remove(0);
		assertEquals("test_create.txt", fileCreated.getName());
		assertEquals(UpdateType.ENTRY_CREATE, fileCreated.getUpdateType());

		// Delete file
		new File(temp.getAbsolutePath() + "/test5").delete();
		Thread.sleep(300);// Wait for update
		assertEquals(1, updates.size());
		FileListlet fileDeleted = updates.remove(0);
		assertEquals("test5", fileDeleted.getName());
		assertEquals(UpdateType.ENTRY_DELETE, fileDeleted.getUpdateType());

		// Modify file
		try (PrintWriter pw = new PrintWriter(temp.getAbsolutePath() + "/logback.xml")) {
			pw.println("some modification");
		}
		Thread.sleep(300);// Wait for update
		FileListlet fileModified = updates.remove(0);
		assertEquals("logback.xml", fileModified.getName());
		assertEquals(UpdateType.ENTRY_MODIFY, fileModified.getUpdateType());

	}

	@Test
	void testCaching() throws IOException {

		// Ensure same list is returned
		assertTrue(fs.list() == fs.list());

		// Cycle between directories
		assertTrue(fs.down("test1"));
		assertTrue(fs.up());
		assertTrue(fs.down("test1"));
		assertTrue(fs.up());
		assertTrue(fs.down("test1"));
		assertTrue(fs.up());
	}

	@Test
	void testConsistencyPwd() throws IOException {

		List<FileListlet> listing1 = new ArrayList<>(fs.list());
		new File(temp.getAbsolutePath() + "/test_create.txt").createNewFile();
		List<FileListlet> listing2 = fs.list();

		assertEquals(listing1.size() + 1, listing2.size());
	}

	@Test
	void testConsistencyCache() throws Exception {

		assertTrue(fs.down("test1"));
		List<FileListlet> listing1 = new ArrayList<>(fs.list());
		assertTrue(fs.up());

		new File(temp.getAbsolutePath() + "/test1/test_create.txt").createNewFile();
		Thread.sleep(500);

		assertTrue(fs.down("test1"));
		List<FileListlet> listing2 = fs.list();

		assertEquals(listing1.size() + 1, listing2.size());
	}
}
