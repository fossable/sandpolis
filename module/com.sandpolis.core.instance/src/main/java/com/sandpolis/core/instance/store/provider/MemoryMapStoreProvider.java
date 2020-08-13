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

import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.core.instance.store.StoreMetadata;

/**
 * {@link MemoryMapStoreProvider} is an ephemeral {@link StoreProvider} that is
 * backed by a {@link HashMap} by default.
 *
 * @since 5.0.0
 */
public class MemoryMapStoreProvider<K, E> implements StoreProvider<E> {

	private final Oid<?> oid;

	private final Map<Object, E> container;

	private final Function<E, Object> idFunction;

	private final Metadata metadata = new Metadata();

	public MemoryMapStoreProvider(Class<E> cls, Oid<?> oid) {
		this(cls, Object::hashCode, oid);
	}

	public MemoryMapStoreProvider(Class<E> cls, Function<E, Object> idFunction, Oid<?> oid) {
		this(cls, idFunction, oid, new HashMap<>());
	}

	public MemoryMapStoreProvider(Class<E> cls, Function<E, Object> idFunction, Oid<?> oid, Map<Object, E> map) {
		this.container = Objects.requireNonNull(map);
		this.idFunction = Objects.requireNonNull(idFunction);
		this.oid = oid;
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
		assert true;
	}

	@Override
	public Oid<?> getOid() {
		return oid;
	}

	private class Metadata implements StoreMetadata {

		@Override
		public int getInitCount() {
			return 1;
		}
	}
}
