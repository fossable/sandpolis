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
package com.sandpolis.core.instance.store.provider;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.sandpolis.core.instance.store.StoreMetadata;

/**
 * A {@link StoreProvider} manages the artifacts of a Store. The
 * {@code StoreProvider} "provides" basic storage of objects via a list-like
 * interface to remove the burden of object management in the {@code Store}
 * itself.<br>
 * <br>
 *
 * An additional benefit of offloading this duty to a provider class is that
 * Stores that rely on a database can be easily unit tested by using a
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
	public Optional<E> get(Object id);

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
		return get(id).isPresent();
	}

	/**
	 * Get the number of elements in the store.
	 *
	 * @return The number of elements in the store
	 */
	default public long count() {
		return enumerate().size();
	}

	public Collection<E> enumerate();

	/**
	 * The streams produced by this method are eagerly loaded. For performance
	 * improvements when filtering, use #stream(String) which delegates
	 * responsibility to the database itself.
	 *
	 * @return A new {@link Stream} over the elements in the store
	 */
	default public Stream<E> stream() {
		return enumerate().stream();
	}

	public Collection<E> enumerate(String query, Object... params);

	default public Stream<E> stream(String query, Object... params) {
		return enumerate(query, params).stream();
	}

	public void initialize();

	public StoreMetadata getMetadata();
	
	public com.sandpolis.core.instance.data.Collection getCollection();
}
