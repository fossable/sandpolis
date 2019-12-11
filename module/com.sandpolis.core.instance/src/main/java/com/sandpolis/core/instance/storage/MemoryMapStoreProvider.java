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
package com.sandpolis.core.instance.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * An ephemeral {@link StoreProvider} that is backed by a {@link Map}.
 *
 * @author cilki
 * @since 5.0.0
 */
public class MemoryMapStoreProvider<K, E> extends ConcurrentStoreProvider<E> implements StoreProvider<E> {

	/**
	 * The backing storage for this {@code StoreProvider}.
	 */
	private final Map<Object, E> map;

	private final Function<E, K> keyFunction;

	public MemoryMapStoreProvider(Class<E> cls, Function<E, K> keyFunction) {
		this(cls, keyFunction, new HashMap<>());
	}

	public MemoryMapStoreProvider(Class<E> cls, Function<E, K> keyFunction, Map<Object, E> map) {
		this.map = Objects.requireNonNull(map);
		this.keyFunction = keyFunction;
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
		mutate(() -> map.put(keyFunction.apply(e), e));
	}

	@Override
	public void remove(E e) {
		mutate(() -> map.remove(keyFunction.apply(e)));
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
