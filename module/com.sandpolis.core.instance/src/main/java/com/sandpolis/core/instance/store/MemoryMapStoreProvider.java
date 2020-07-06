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
package com.sandpolis.core.instance.store;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.sandpolis.core.instance.data.StateObject;

/**
 * An ephemeral {@link StoreProvider} that is backed by a {@link Map}.
 *
 * @author cilki
 * @since 5.0.0
 */
public class MemoryMapStoreProvider<K, E extends StateObject> extends ConcurrentStoreProvider<E>
		implements StoreProvider<E> {

	/**
	 * The backing storage for this {@code StoreProvider}.
	 */
	private final Map<Integer, E> map;

	public MemoryMapStoreProvider(Class<E> cls) {
		this(cls, new HashMap<>());
	}

	public MemoryMapStoreProvider(Class<E> cls, Map<Integer, E> map) {
		this.map = Objects.requireNonNull(map);
	}

	@Override
	public Optional<E> get(Object id) {
		return Optional.ofNullable(map.get(id));
	}

	@Override
	public Stream<E> unsafeStream() {
		beginStream();
		return map.values().stream().onClose(() -> endStream());
	}

	@Override
	public void add(E e) {
		mutate(() -> map.put(e.tag(), e));
	}

	@Override
	public void remove(E e) {
		mutate(() -> map.remove(e.tag()));
	}

	@Override
	public void removeIf(Predicate<E> condition) {
		mutate(() -> map.values().removeIf(condition));
	}

	@Override
	public void clear() {
		mutate(() -> map.clear());
	}

}
