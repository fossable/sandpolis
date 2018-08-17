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
package com.sandpolis.core.storage.ormlite;

import java.io.IOException;
import java.sql.SQLException;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.sandpolis.core.instance.storage.database.Database;

/**
 * A factory for producing initialized Ormlite databases.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class OrmliteDatabaseFactory {

	/**
	 * Initialize a new SQLite database with Ormlite.
	 * 
	 * @param persist A list of classes that will be persisted or {@code null} for
	 *                none
	 * @param db      The database to be initialized
	 * @return The initialized database
	 */
	public static Database sqlite(Class<?>[] persist, Database db) throws SQLException, IOException {
		return db
				.init(new OrmliteConnection(new JdbcConnectionSource(db.getUrl(), db.getUsername(), db.getPassword())));
	}

	/**
	 * Initialize a new MySQL database with Ormlite.
	 * 
	 * @param persist A list of classes that will be persisted or {@code null} for
	 *                none
	 * @param db      The database to be initialized
	 * @return The initialized database
	 */
	public static Database mysql(Class<?>[] persist, Database db) throws SQLException, IOException {
		return db
				.init(new OrmliteConnection(new JdbcConnectionSource(db.getUrl(), db.getUsername(), db.getPassword())));
	}
}
