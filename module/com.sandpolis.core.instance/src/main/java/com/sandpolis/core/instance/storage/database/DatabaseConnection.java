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

import com.sandpolis.core.instance.storage.StoreProvider;

/**
 * Represents the connection to a {@link Database}.
 * 
 * @author cilki
 * @since 5.0.0
 */
public abstract class DatabaseConnection implements AutoCloseable {

	/**
	 * Indicated whether the connection is currently open.
	 * 
	 * @return The connection status
	 */
	public abstract boolean isOpen();

	/**
	 * Obtain a new {@link StoreProvider} for this database.
	 * 
	 * @param cls The class type that the provider will manage
	 * @return A new provider for the given class
	 */
	public abstract <E> StoreProvider<E> provider(Class<E> cls, String idField);

}
