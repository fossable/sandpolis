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
package com.sandpolis.core.storage.hibernate;

import org.hibernate.cfg.Configuration;

import com.sandpolis.core.instance.storage.database.Database;

/**
 * A factory for producing initialized Hibernate databases.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class HibernateDatabaseFactory {
	private HibernateDatabaseFactory() {
	}

	/**
	 * Initialize a new SQLite database with Hibernate.
	 * 
	 * @param persist A list of classes that will be persisted or {@code null} for
	 *                none
	 * @param db      The database to be initialized
	 * @return The initialized database
	 */
	public static Database sqlite(Class<?>[] persist, Database db) {
		if (db == null)
			throw new IllegalArgumentException();
		if (persist == null)
			persist = new Class[0];

		Configuration conf = new Configuration()
				// Set the SQLite database driver
				.setProperty("connection.driver_class", "org.sqlite.JDBC")

				// Set the SQLite dialect
				.setProperty("hibernate.dialect", "org.hibernate.dialect.SQLiteDialect")

				// Set the credentials
				.setProperty("hibernate.connection.username", db.getUsername())
				.setProperty("hibernate.connection.password", db.getPassword())

				// Set the database URL
				.setProperty("hibernate.connection.url", db.getUrl())

				.setProperty("hibernate.show_sql", "true")

				// Set pool options
				.setProperty("hibernate.c3p0.min_size", "0").setProperty("hibernate.c3p0.max_size", "1")

				// Set additional options
				.setProperty("hibernate.connection.shutdown", "true").setProperty("hibernate.hbm2ddl.auto", "create")
				.setProperty("hibernate.current_session_context_class", "thread");
		for (Class<?> cls : persist)
			conf.addAnnotatedClass(cls);

		return db.init(new HibernateConnection(conf.buildSessionFactory()));
	}

	/**
	 * Initialize a new MySQL database with Hibernate.
	 * 
	 * @param persist A list of classes that will be persisted or {@code null} for
	 *                none
	 * @param db      The database to be initialized
	 * @return The initialized database
	 */
	public static Database mysql(Class<?>[] persist, Database db) {
		if (db == null)
			throw new IllegalArgumentException();
		if (persist == null)
			persist = new Class[0];

		Configuration conf = new Configuration()
				// Set the MySQL database driver
				.setProperty("connection.driver_class", "com.mysql.jdbc.Driver")

				// Set the MySQL dialect
				.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL5Dialect")

				// Set the credentials
				.setProperty("hibernate.connection.username", db.getUsername())
				.setProperty("hibernate.connection.password", db.getPassword())

				// Set the database URL
				.setProperty("hibernate.connection.url", db.getUrl())

				// Set pool options

				// Set additional options
				.setProperty("hibernate.hbm2ddl.auto", "create")
				.setProperty("hibernate.current_session_context_class", "thread");
		for (Class<?> cls : persist)
			conf.addAnnotatedClass(cls);

		return db.init(new HibernateConnection(conf.buildSessionFactory()));
	}
}
