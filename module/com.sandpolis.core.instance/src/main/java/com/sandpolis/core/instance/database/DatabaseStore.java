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
package com.sandpolis.core.instance.database;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.database.DatabaseStore.DatabaseStoreConfig;
import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.instance.store.MemoryMapStoreProvider;
import com.sandpolis.core.instance.store.StoreConfig;

/**
 * The {@link DatabaseStore} manages various types of SQL databases.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class DatabaseStore extends MapStore<Database, DatabaseStoreConfig> {

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
		if (main != null)
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

		public Database main;

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(Database.class);
			DatabaseStore.this.main = main;
		}

	}

	public static final DatabaseStore DatabaseStore = new DatabaseStore();
}
