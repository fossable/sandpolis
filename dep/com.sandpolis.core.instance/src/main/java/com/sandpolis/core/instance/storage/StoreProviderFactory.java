/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
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
package com.sandpolis.core.instance.storage;

import com.sandpolis.core.instance.storage.database.Database;

/**
 * A singleton factory for new {@link StoreProvider} instances.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class StoreProviderFactory {
	private StoreProviderFactory() {
	}

	/**
	 * Build a new persistent {@link StoreProvider} for the given class.
	 * 
	 * @param cls The class that will be managed by the provider
	 * @param db  The target database which must be open
	 * @return The new store provider
	 */
	public static <E> StoreProvider<E> database(Class<E> cls, Database db) {
		if (cls == null)
			throw new IllegalArgumentException();
		if (db == null)
			throw new IllegalArgumentException();
		if (!db.isOpen())
			throw new IllegalArgumentException();

		return db.getConnection().provider(cls);
	}

	/**
	 * Build a new non-persistent {@link StoreProvider} for the given class.
	 * 
	 * @param cls The class that will be managed by the provider
	 * @return The new store provider
	 */
	public static <E> StoreProvider<E> memoryList(Class<E> cls) {
		if (cls == null)
			throw new IllegalArgumentException();

		return new MemoryListStoreProvider<E>(cls);
	}

	/**
	 * Build a new non-persistent {@link StoreProvider} for the given class.
	 * 
	 * @param cls The class that will be managed by the provider
	 * @return The new store provider
	 */
	public static <E> StoreProvider<E> memoryMap(Class<E> cls) {
		if (cls == null)
			throw new IllegalArgumentException();

		return new MemoryMapStoreProvider<E>(cls);
	}
}
