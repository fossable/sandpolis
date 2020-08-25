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

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.core.instance.store.CollectionStore;
import com.sandpolis.core.instance.store.StoreMetadata;

/**
 * A {@link StoreProvider} manages the artifacts of a {@link CollectionStore}.
 * It "provides" ephemeral and persistent storage of objects via a
 * collection-like API.
 *
 * <p>
 * An additional benefit of offloading this duty to a provider class is that
 * stores that rely on a database can be easily unit tested by using a
 * memory-only implementation such as {@link MemoryListStoreProvider}.
 *
 * <p>
 * Note: default implementations should be overridden if more efficient ones
 * exist.
 *
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
	 * Remove an element from the store's backing storage by value.
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
	 * Indicate whether an element exists in the store.
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

	/**
	 * Build a new {@link java.util.Collection} with all elements in the store.
	 *
	 * @return A new {@link java.util.Collection}
	 */
	public java.util.Collection<E> enumerate();

	public java.util.Collection<E> enumerate(String query, Object... params);

	/**
	 * Build a new {@link Stream} over all elements in the store.
	 * <p>
	 * The streams produced by this method are eagerly loaded. For performance
	 * improvements when filtering, use {@link #stream(String, Object...)}.
	 *
	 * @return A new {@link Stream}
	 */
	default public Stream<E> stream() {
		return enumerate().stream();
	}

	default public Stream<E> stream(String query, Object... params) {
		return enumerate(query, params).stream();
	}

	public void initialize();

	public StoreMetadata getMetadata();

	public Oid<?> getOid();
}
