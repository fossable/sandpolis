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
import com.sandpolis.core.instance.storage.database.DatabaseFactory.IDatabaseFactory;

/**
 * A factory for producing initialized Hibernate databases.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class HibernateDatabaseFactory implements IDatabaseFactory {

	@Override
	public Database sqlite(Class<?>[] persist, Database db) {
		if (db == null)
			throw new IllegalArgumentException();
		if (persist == null)
			persist = new Class[0];

		Configuration conf = new Configuration()
				// Set the SQLite database driver
				.setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC")

				// Set the SQLite dialect
				.setProperty("hibernate.dialect", "org.hibernate.dialect.SQLiteDialect")

				// Set the credentials
				.setProperty("hibernate.connection.username", db.getUsername())
				.setProperty("hibernate.connection.password", db.getPassword())

				// Set the database URL
				.setProperty("hibernate.connection.url", db.getUrl())

				// Set pool options
				.setProperty("hibernate.c3p0.min_size", "0").setProperty("hibernate.c3p0.max_size", "1")

				// Set additional options
				.setProperty("hibernate.connection.shutdown", "true")
				.setProperty("hibernate.globally_quoted_identifiers", "true")

				// This property cannot be changed because SQLite does not support altering a
				// table's unique constrants
				.setProperty("hibernate.hbm2ddl.auto", "create");
		for (Class<?> cls : persist)
			conf.addAnnotatedClass(cls);

		return db.init(new HibernateConnection(conf.buildSessionFactory()));
	}

	@Override
	public Database mysql(Class<?>[] persist, Database db) {
		if (db == null)
			throw new IllegalArgumentException();
		if (persist == null)
			persist = new Class[0];

		Configuration conf = new Configuration()
				// Set the MySQL database driver
				.setProperty("hibernate.connection.driver_class", "org.mariadb.jdbc.Driver")

				// Set the MySQL dialect
				.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL5Dialect")

				// Set the credentials
				.setProperty("hibernate.connection.username", db.getUsername())
				.setProperty("hibernate.connection.password", db.getPassword())

				// Set the database URL
				.setProperty("hibernate.connection.url", db.getUrl())

				// Set pool options
				.setProperty("hibernate.c3p0.min_size", "0").setProperty("hibernate.c3p0.max_size", "20")

				// Set additional options
				.setProperty("hibernate.globally_quoted_identifiers", "true")
				.setProperty("hibernate.hbm2ddl.auto", "create");
		for (Class<?> cls : persist)
			conf.addAnnotatedClass(cls);

		return db.init(new HibernateConnection(conf.buildSessionFactory()));
	}

	@Override
	public Database h2(Class<?>[] persist, Database db) {
		if (db == null)
			throw new IllegalArgumentException();
		if (persist == null)
			persist = new Class[0];

		Configuration conf = new Configuration()
				// Set the H2 database driver
				.setProperty("hibernate.connection.driver_class", "org.h2.Driver")

				// Set the H2 dialect
				.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")

				// Set the credentials
				.setProperty("hibernate.connection.username", db.getUsername())
				.setProperty("hibernate.connection.password", db.getPassword())

				// Set the database URL
				.setProperty("hibernate.connection.url", db.getUrl())

				// Set pool options
				.setProperty("hibernate.c3p0.min_size", "0").setProperty("hibernate.c3p0.max_size", "1")

				// Set additional options
				.setProperty("hibernate.connection.shutdown", "true")
				.setProperty("hibernate.globally_quoted_identifiers", "true")
				.setProperty("hibernate.hbm2ddl.auto", "create");
		for (Class<?> cls : persist)
			conf.addAnnotatedClass(cls);

		return db.init(new HibernateConnection(conf.buildSessionFactory()));
	}
}