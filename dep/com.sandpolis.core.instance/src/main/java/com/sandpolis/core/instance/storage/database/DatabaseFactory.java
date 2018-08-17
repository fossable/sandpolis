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
import java.lang.reflect.Method;
import java.net.URISyntaxException;

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
	 * The Hibernate factory class.
	 */
	private static Class<?> hibernateFactory;

	/**
	 * The Ormlite factory class.
	 */
	private static Class<?> ormliteFactory;

	static {
		try {
			hibernateFactory = Class.forName("com.sandpolis.core.storage.hibernate.HibernateDatabaseFactory");
		} catch (ClassNotFoundException ignore) {
			hibernateFactory = null;
		}

		try {
			ormliteFactory = Class.forName("com.sandpolis.core.storage.ormlite.OrmliteDatabaseFactory");
		} catch (ClassNotFoundException ignore) {
			ormliteFactory = null;
		}
	}

	/**
	 * Create a new uninitialized {@link Database} from the given {@link File}.
	 * 
	 * @param file The source file which will be created if it does not exist
	 * @return An uninitialized {@code Database}
	 * @throws IOException If the file creation fails
	 */
	public static Database create(File file) throws IOException {
		if (file == null)
			throw new IllegalArgumentException("Null argument");
		if (file.isDirectory())
			throw new IllegalArgumentException("Invalid database file");
		if (!file.exists() && !file.createNewFile())
			throw new IOException("Failed to create database file");

		try {
			return new Database("jdbc:sqlite:file:" + file.getAbsolutePath());
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

		String type = database.getFile() == null ? "mysql" : "sqlite";

		try {
			return (Database) getFactoryMethod(type).invoke(null, persist, database);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the {@link Method} that will yield a new {@link Database} for the
	 * configured provider.
	 * 
	 * @param dbType The type of database
	 * @return The factory method
	 */
	private static Method getFactoryMethod(String dbType) throws NoSuchMethodException, SecurityException {
		switch (Config.DB_PROVIDER) {
		case "":
		case "default":
		case "ormlite":
			if (ormliteFactory == null)
				throw new RuntimeException("Ormlite not found on classpath");
			return ormliteFactory.getMethod(dbType, Class[].class, Database.class);
		case "hibernate":
			if (hibernateFactory == null)
				throw new RuntimeException("Hibernate not found on classpath");
			return hibernateFactory.getMethod(dbType, Class[].class, Database.class);
		default:
			throw new RuntimeException();
		}
	}
}
