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

import java.util.function.Predicate;
import java.util.stream.Stream;

import com.sandpolis.core.instance.Store;

/**
 * A {@link StoreProvider} manages the artifacts of a {@link Store}. The
 * {@code StoreProvider} "provides" basic storage of objects via a list-like
 * interface to remove the burden of object management in the {@code Store}
 * itself.<br>
 * <br>
 * 
 * An additional benefit of offloading this duty to a provider class is that
 * {@link Store}s that rely on a database can be easily unit tested by using a
 * memory-only implementation such as {@link MemoryListStoreProvider}.<br>
 * <br>
 * Note: default implementations should be overridden if more efficient
 * implementations exist.
 * 
 * @author cilki
 * @since 5.0.0
 */
public interface StoreProvider<E> {

	/**
	 * Add an object to the store's backing storage.
	 * 
	 * @param e The item to add
	 */
	public void add(E e);

	/**
	 * Retrieve an object from the store's backing storage by the primary
	 * identifier.
	 * 
	 * @param id The primary key of the item to retrieve
	 * @return The requested object
	 */
	public E get(Object id);

	/**
	 * Retrieve an object from the store's backing storage by the given identifier.
	 * 
	 * @param field The field name
	 * @param id    The field value of the item to retrieve
	 * @return The requested object
	 */
	public E get(String field, Object id);

	/**
	 * Remove an element from the store's backing storage.
	 * 
	 * @param e The element to remove
	 */
	public void remove(E e);

	/**
	 * Remove all elements that satisfy the given condition.
	 * 
	 * @param condition The removal condition
	 */
	public void removeIf(Predicate<E> condition);

	/**
	 * Remove all elements from the store's backing storage.
	 */
	public void clear();

	/**
	 * Indicates whether an element exists in the store.
	 * 
	 * @param id The primary key of the element
	 * @return Whether the requested object exists in the store
	 */
	default public boolean exists(Object id) {
		return get(id) != null;
	}

	/**
	 * Indicates whether an element exists in the store.
	 * 
	 * @param field The field name
	 * @param id    The field value of the element
	 * @return Whether the requested object exists in the store
	 */
	default public boolean exists(String field, Object id) {
		return get(field, id) != null;
	}

	/**
	 * Get the number of elements in the store.
	 * 
	 * @return The number of elements in the store
	 */
	default public long count() {
		try (Stream<E> stream = stream()) {
			return stream.count();
		}
	}

	/**
	 * Get a {@link Stream} over the elements in the store. <b>The stream MUST be
	 * properly closed, otherwise the store will become permanently immutable</b>.
	 * Always use the following idiom:
	 * 
	 * <pre>
	 * try (Stream stream = provider.stream()) {
	 * 	...
	 * }
	 * </pre>
	 * 
	 * @return A new {@link Stream} over the elements in the store
	 */
	public Stream<E> stream();

	// TODO This may not be needed
	default public void transaction(Runnable operation) {
		operation.run();
	}

}
