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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;

import com.sandpolis.core.instance.Config;

/**
 * A singleton factory for new {@link Database} instances.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class DatabaseFactory {
	private DatabaseFactory() {
	}

	/**
	 * The Hibernate database factory.
	 */
	private static final IDatabaseFactory hibernateFactory;

	/**
	 * The Ormlite database factory.
	 */
	private static final IDatabaseFactory ormliteFactory;

	static {
		hibernateFactory = loadFactory("com.sandpolis.core.storage.hibernate.HibernateDatabaseFactory");
		ormliteFactory = loadFactory("com.sandpolis.core.storage.ormlite.OrmliteDatabaseFactory");
	}

	/**
	 * Load a database factory.
	 */
	private static IDatabaseFactory loadFactory(String path) {
		try {
			return (IDatabaseFactory) Class.forName(path).getConstructor().newInstance();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Create a new uninitialized {@link Database} from the given {@link File}.
	 * 
	 * @param type The database type
	 * @param file The source file which will be created if it does not exist
	 * @return An uninitialized {@code Database}
	 * @throws IOException If the file creation fails
	 */
	public static Database create(String type, File file) throws IOException {
		Objects.requireNonNull(type);
		Objects.requireNonNull(file);
		if (file.isDirectory())
			throw new IllegalArgumentException("Invalid database file");
		if (!file.exists() && !file.createNewFile())
			throw new IOException("Failed to create database file");

		try {
			return new Database(String.format("jdbc:%s:file:%s", type, file.getAbsolutePath()));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create a new uninitialized {@link Database} from the given URL.
	 * 
	 * @param url The source url
	 * @return An uninitialized {@code Database}
	 * @throws URISyntaxException If the url format is invalid
	 */
	public static Database create(String url) throws URISyntaxException {
		return new Database(url);
	}

	/**
	 * Initialize an uninitialized {@link Database}.
	 * 
	 * @param database The uninitialized database
	 * @param persist  A list of persistent classes
	 * @return The initialized database
	 */
	public static Database init(Database database, Class<?>[] persist) {
		if (database.isOpen())
			// Already initialized
			throw new IllegalArgumentException();

		IDatabaseFactory fac;
		switch (Config.DB_PROVIDER) {
		case "":
		case "default":
		case "ormlite":
			if (ormliteFactory == null)
				throw new RuntimeException("Ormlite not found on classpath");

			fac = ormliteFactory;
			break;
		case "hibernate":
			if (hibernateFactory == null)
				throw new RuntimeException("Hibernate not found on classpath");
			fac = hibernateFactory;
			break;
		default:
			throw new RuntimeException();
		}

		try {
			switch (database.getType()) {
			case "mysql":
				return fac.mysql(persist, database);
			case "sqlite":
				return fac.sqlite(persist, database);
			case "h2":
				return fac.h2(persist, database);
			default:
				throw new RuntimeException();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * A contract that every database factory should implement.<br>
	 * <br>
	 * TODO: Think of a better name for this interface.
	 * 
	 * @author cilki
	 * @since 5.0.0
	 */
	public static interface IDatabaseFactory {

		/**
		 * Initialize a new SQLite database.
		 * 
		 * @param persist A list of classes that will be persisted or {@code null} for
		 *                none
		 * @param db      The database to be initialized
		 * @return The initialized database
		 */
		public Database sqlite(Class<?>[] persist, Database db) throws Exception;

		/**
		 * Initialize a new MySQL database.
		 * 
		 * @param persist A list of classes that will be persisted or {@code null} for
		 *                none
		 * @param db      The database to be initialized
		 * @return The initialized database
		 */
		public Database mysql(Class<?>[] persist, Database db) throws Exception;

		/**
		 * Initialize a new H2 database.
		 * 
		 * @param persist A list of classes that will be persisted or {@code null} for
		 *                none
		 * @param db      The database to be initialized
		 * @return The initialized database
		 */
		public Database h2(Class<?>[] persist, Database db) throws Exception;
	}
}
