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
package com.sandpolis.core.instance.store.database;

import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Store;
import com.sandpolis.core.instance.storage.Database;
import com.sandpolis.core.instance.storage.DatabaseFactory;
import com.sandpolis.core.instance.storage.StoreProvider;
import com.sandpolis.core.instance.storage.StoreProviderFactory;

/**
 * The {@link DatabaseStore} manages various types of SQL databases.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class DatabaseStore extends Store {
	private DatabaseStore() {
	}

	private static final Logger log = LoggerFactory.getLogger(DatabaseStore.class);

	private static StoreProvider<Database> provider;

	/**
	 * The main instance database.
	 */
	private static Database main;

	public static void init(StoreProvider<Database> provider) {
		if (provider == null)
			throw new IllegalArgumentException();

		DatabaseStore.provider = provider;
	}

	public static void load(Database main, Class<?>[] cls) {
		if (main == null)
			throw new IllegalArgumentException();

		if (!main.isOpen()) {
			DatabaseFactory.init(main, cls);
		}

		init(StoreProviderFactory.database(Database.class, main));
		DatabaseStore.main = main;
	}

	public static void close() throws Exception {
		main.close();
	}

	/**
	 * Get the instance database.
	 * 
	 * @return The instance database or {@code null}
	 */
	public static Database main() {
		return main;
	}

	/**
	 * Add a {@link Database} to the store.
	 * 
	 * @param db A new database
	 */
	public static void add(Database db) {
		provider.add(db);
	}

	/**
	 * Get a new database stream.
	 * 
	 * @return A stream over the elements in this store
	 */
	public static Stream<Database> stream() {
		return provider.stream();
	}

}
