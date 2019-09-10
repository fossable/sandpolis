/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.core.instance.store.database;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.storage.MemoryMapStoreProvider;
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

	public DatabaseStore() {
		super(log);
	}

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

		return (DatabaseStore) super.init(null);
	}

	public final class DatabaseStoreConfig extends StoreConfig {

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(Database.class, Database::getUrl);
		}

		public Class<?>[] entities;

		@Override
		public void persistent(Database database) {
			main = DatabaseFactory.init(database, entities);
			provider = main.getConnection().provider(Database.class, "id");
		}
	}

	public static final DatabaseStore DatabaseStore = new DatabaseStore();
}
