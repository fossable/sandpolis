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

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.storage.MemoryListStoreProvider;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.instance.storage.database.DatabaseFactory;
import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.instance.store.StoreBase.StoreConfig;
import com.sandpolis.core.instance.store.database.DatabaseStore.DatabaseStoreConfig;

/**
 * The {@link DatabaseStore} manages various types of SQL databases.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class DatabaseStore extends MapStore<String, Database, DatabaseStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(DatabaseStore.class);

	/**
	 * The main instance database.
	 */
	private Database main;

	@Override
	public void close() throws Exception {
		main.close();
	}

	/**
	 * Get the instance database.
	 * 
	 * @return The instance database or {@code null}
	 */
	public Database main() {
		return main;
	}

	@Override
	public DatabaseStore init(Consumer<DatabaseStoreConfig> configurator) {
		var config = new DatabaseStoreConfig();
		configurator.accept(config);

		DatabaseFactory.init(config.main, null);
		this.main = config.main;

		return (DatabaseStore) super.init(null);
	}

	public final class DatabaseStoreConfig extends StoreConfig {

		public Database main;

		@Override
		public void ephemeral() {
			provider = new MemoryListStoreProvider<>(Database.class);
		}

	}

	public static final DatabaseStore DatabaseStore = new DatabaseStore();
}
