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

import com.sandpolis.core.instance.DocumentBindings;

/**
 * @author cilki
 * @since 3.0.0
 */
public final class Database extends DocumentBindings.Profile.Instance.Server.Database implements AutoCloseable {

	private DatabaseConnection connection;

	/**
	 * Construct a new {@link Database} from the given URL with blank credentials.
	 *
	 * @param url The database url
	 */
	public Database(String url, DatabaseConnection connection) {
		super(null);
		this.connection = connection;
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
}
