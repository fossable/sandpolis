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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.sandpolis.core.instance.state.Collection;
import com.sandpolis.core.instance.state.Document;
import com.sandpolis.core.instance.store.StoreMetadata;

/**
 * An ephemeral {@link StoreProvider} that is backed by a {@link List}. Read
 * operations require a sequential search, so this provider is suitable for a
 * small number of items or infrequent reads.
 *
 * @author cilki
 * @since 5.0.0
 */
public class MemoryListStoreProvider<K, E> implements StoreProvider<E> {

	private final Collection collection = new Collection((Document) null);

	private final List<E> container;

	private final Function<E, Object> idFunction;

	public MemoryListStoreProvider(Class<E> cls, Function<E, Object> idFunction) {
		this(cls, idFunction, new ArrayList<>());
	}

	public MemoryListStoreProvider(Class<E> cls, Function<E, Object> idFunction, List<E> list) {
		this.container = Objects.requireNonNull(list);
		this.idFunction = Objects.requireNonNull(idFunction);
	}

	@Override
	public synchronized Optional<E> get(Object id) {

		for (E e : container) {
			if (id.equals(idFunction.apply(e)))
				return Optional.of(e);
		}
		return Optional.empty();

	}

	@Override
	public synchronized void add(E e) {
		// TODO check for duplicate IDs
		container.add(e);
	}

	@Override
	public synchronized void remove(E e) {
		container.remove(e);
	}

	@Override
	public synchronized void removeIf(Predicate<E> condition) {
		container.removeIf(condition);
	}

	@Override
	public synchronized void clear() {
		container.clear();
	}

	@Override
	public synchronized java.util.Collection<E> enumerate() {
		return List.copyOf(container);
	}

	@Override
	public synchronized java.util.Collection<E> enumerate(String query, Object... params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize() {
	}

	@Override
	public StoreMetadata getMetadata() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection getCollection() {
		return collection;
	}

}
