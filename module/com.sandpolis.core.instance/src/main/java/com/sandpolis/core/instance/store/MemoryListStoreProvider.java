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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.sandpolis.core.instance.data.StateObject;

/**
 * An ephemeral {@link StoreProvider} that is backed by a {@link List}.
 *
 * @author cilki
 * @since 5.0.0
 */
public class MemoryListStoreProvider<K, E extends StateObject> extends ConcurrentStoreProvider<E>
		implements StoreProvider<E> {

	/**
	 * The backing storage for this {@link StoreProvider}.
	 */
	private final List<E> list;

	public MemoryListStoreProvider(Class<E> cls) {
		this(cls, new ArrayList<>());
	}

	public MemoryListStoreProvider(Class<E> cls, List<E> list) {
		this.list = Objects.requireNonNull(list);
	}

	@Override
	public Optional<E> get(Object id) {
		beginStream();
		try {
			for (E e : list) {
				if (id.equals(e.tag()))
					return Optional.of(e);
			}
			return Optional.empty();
		} finally {
			endStream();
		}
	}

	@Override
	public Stream<E> unsafeStream() {
		beginStream();
		return list.stream().onClose(() -> endStream());
	}

	@Override
	public void add(E e) {
		// TODO check for duplicate IDs
		mutate(() -> list.add(e));
	}

	@Override
	public void remove(E e) {
		mutate(() -> list.remove(e));
	}

	@Override
	public void removeIf(Predicate<E> condition) {
		mutate(() -> list.removeIf(condition));
	}

	@Override
	public void clear() {
		mutate(() -> list.clear());
	}
}
