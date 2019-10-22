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
package com.sandpolis.core.instance.storage.database;

import java.util.Objects;

/**
 * @author cilki
 * @since 3.0.0
 */
public final class Database implements AutoCloseable {

	private DatabaseConnection connection;

	private String url;

	/**
	 * Construct a new {@link Database} from the given URL with blank credentials.
	 *
	 * @param url The database url
	 */
	public Database(String url, DatabaseConnection connection) {
		this.url = Objects.requireNonNull(url);
		this.connection = Objects.requireNonNull(connection);
	}

	/**
	 * Get the database's {@link DatabaseConnection}.
	 *
	 * @return The connection object
	 */
	public DatabaseConnection getConnection() {
		return connection;
	}

	/**
	 * Indicates the connection status.
	 *
	 * @return Whether the database is connected
	 */
	public boolean isOpen() {
		return connection != null && connection.isOpen();
	}

	@Override
	public void close() throws Exception {
		try {
			if (connection != null)
				connection.close();
		} finally {
			connection = null;
		}
	}

	/**
	 * Get the database URL in standard format.
	 *
	 * @return The database URL
	 */
	public String getUrl() {
		return url;
	}
}
