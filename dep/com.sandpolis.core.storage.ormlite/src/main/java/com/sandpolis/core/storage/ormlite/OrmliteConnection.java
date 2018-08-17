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

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.sandpolis.core.instance.storage.StoreProvider;
import com.sandpolis.core.instance.storage.database.DatabaseConnection;

/**
 * Represents the connection to an Ormlite database.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class OrmliteConnection extends DatabaseConnection {

	private ConnectionSource connection;

	public OrmliteConnection(ConnectionSource connection) throws IOException {
		if (connection == null)
			throw new IllegalArgumentException();

		this.connection = connection;
	}

	/**
	 * Drop the table for the given class.
	 * 
	 * @param cls The class to truncate
	 * @return True if the table was dropped
	 */
	public <E> boolean truncate(Class<E> cls) {
		try {
			TableUtils.dropTable(connection, cls, true);
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	@Override
	public void close() throws Exception {
		try {
			connection.close();
		} finally {
			connection = null;
		}
	}

	@Override
	public boolean isOpen() {
		return connection.isOpen("");// TODO what table???
	}

	@Override
	public <E> StoreProvider<E> provider(Class<E> cls) {
		try {
			TableUtils.createTableIfNotExists(connection, cls);
			return new OrmliteStoreProvider<>(cls, connection);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
