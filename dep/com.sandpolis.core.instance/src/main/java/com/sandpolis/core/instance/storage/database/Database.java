/******************************************************************************
 *                                                                            *
 *                    Copyright 2016 Subterranean Security                    *
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
import java.net.URI;
import java.net.URISyntaxException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * A {@link Database} is a high-level representation of a SQL database which is
 * managed by an ORM.<br>
 * <br>
 * 
 * In some cases, the {@link Database} object itself is also stored in another
 * database or within itself.
 * 
 * @author cilki
 * @since 3.0.0
 */
@Entity
public final class Database implements AutoCloseable {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int db_id;

	/**
	 * The database url in standard format.
	 */
	@Column
	protected String url;

	/**
	 * The database username.
	 */
	@Column
	protected String username;

	/**
	 * The database password.
	 */
	@Column
	protected String password;

	/**
	 * A timestamp of the last successful connection.
	 */
	@Column
	private long timestamp;

	/**
	 * The database connection.
	 */
	@Transient
	private DatabaseConnection connection;

	/**
	 * Construct a new {@link Database} from the given URL with blank credentials.
	 * 
	 * @param url The database url
	 * @throws URISyntaxException If the url format is incorrect
	 */
	public Database(String url) throws URISyntaxException {
		if (url == null)
			throw new IllegalArgumentException();

		this.url = new URI(url).toString();
		this.username = "";
		this.password = "";
	}

	/**
	 * Construct a new {@link Database} from the given URL with the given
	 * credentials.
	 * 
	 * @param url      The database url
	 * @param username The database username
	 * @param password The database password
	 * @throws URISyntaxException If the url format is incorrect
	 */
	public Database(String url, String username, String password) throws URISyntaxException {
		this(url);
		if (username == null)
			throw new IllegalArgumentException();
		if (password == null)
			throw new IllegalArgumentException();

		this.username = username;
		this.password = password;
	}

	/**
	 * Initialize the database with the given connection.
	 * 
	 * @param connection An established connection
	 * @return {@code this}
	 */
	public Database init(DatabaseConnection connection) {
		if (connection == null)
			throw new IllegalArgumentException();

		this.connection = connection;
		return this;
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
	 * @return True if the database is connected; false otherwise
	 */
	public boolean isOpen() {
		return connection != null && connection.isOpen();
	}

	/**
	 * Get the local database file if it exists.
	 * 
	 * @return A {@link File} representing the location of the local database file
	 *         or {@code null} if the connection is remote
	 */
	public File getFile() {
		if (url.contains("file:"))
			return new File(url.substring(url.indexOf("file:") + 5));
		return null;
	}

	/**
	 * Get the database URL in standard format.
	 * 
	 * @return The database URL
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Get the database username.
	 * 
	 * @return The username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Get the database password.
	 * 
	 * @return The password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Get the timestamp of the last successful connection.
	 * 
	 * @return The connection timestamp
	 */
	public long getTimestamp() {
		return timestamp;
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

}
