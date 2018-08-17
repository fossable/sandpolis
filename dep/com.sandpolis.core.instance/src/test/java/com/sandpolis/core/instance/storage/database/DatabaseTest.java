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
package com.sandpolis.core.instance.storage.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.storage.database.Database;

class DatabaseTest {

	@Test
	void testState() throws Exception {
		try (Database db = new Database("jdbc:sqlite:file:/home/test.db")) {
			assertFalse(db.isOpen());
			assertEquals("/home/test.db", db.getFile().getAbsolutePath());
		}

		try (Database db = new Database("jdbc:mysql:192.168.1.1/database")) {
			assertFalse(db.isOpen());
			assertNull(db.getFile());
			assertNotNull(db.getUrl());
		}
	}

	@Test
	void testInvalidUrls() {
		assertThrows(URISyntaxException.class, () -> new Database("invalid url"));
	}

}
