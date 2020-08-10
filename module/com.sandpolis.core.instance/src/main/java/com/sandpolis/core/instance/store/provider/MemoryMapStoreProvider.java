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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.sandpolis.core.instance.state.Collection;
import com.sandpolis.core.instance.state.Document;
import com.sandpolis.core.instance.store.StoreMetadata;

/**
 * An ephemeral {@link StoreProvider} that is backed by a {@link Map}.
 *
 * @author cilki
 * @since 5.0.0
 */
public class MemoryMapStoreProvider<K, E> implements StoreProvider<E> {

	private final Collection collection = new Collection((Document) null);

	private final Map<Object, E> container;

	private final Function<E, Object> idFunction;

	private final Metadata metadata = new Metadata();

	public MemoryMapStoreProvider(Class<E> cls, Function<E, Object> idFunction) {
		this(cls, idFunction, new HashMap<>());
	}

	public MemoryMapStoreProvider(Class<E> cls, Function<E, Object> idFunction, Map<Object, E> map) {
		this.container = Objects.requireNonNull(map);
		this.idFunction = Objects.requireNonNull(idFunction);
	}

	@Override
	public synchronized Optional<E> get(Object id) {
		return Optional.ofNullable(container.get(id));
	}

	@Override
	public synchronized void add(E e) {
		container.put(idFunction.apply(e), e);
	}

	@Override
	public synchronized void remove(E e) {
		container.remove(idFunction.apply(e));
	}

	@Override
	public synchronized void removeIf(Predicate<E> condition) {
		container.values().removeIf(condition);
	}

	@Override
	public synchronized void clear() {
		container.clear();
	}

	@Override
	public synchronized java.util.Collection<E> enumerate() {
		return container.values();
	}

	@Override
	public synchronized java.util.Collection<E> enumerate(String query, Object... params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StoreMetadata getMetadata() {
		return metadata;
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection getCollection() {
		return collection;
	}

	private class Metadata implements StoreMetadata {

		@Override
		public int getInitCount() {
			return 1;
		}
	}
}
