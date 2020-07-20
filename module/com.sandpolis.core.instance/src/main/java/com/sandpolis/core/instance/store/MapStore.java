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

import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;

public abstract class MapStore<V, E extends StoreConfig> extends StoreBase<E> {

	protected StoreProvider<V> provider;

	protected MapStore(Logger log) {
		super(log);
	}

	public void add(V item) {
		provider.add(item);
	}

	@Override
	public void close() throws Exception {
		provider = null;
	}

	/**
	 * Determine how many elements the store contains.
	 *
	 * @return The number of elements in the store
	 */
	public long count() {
		return provider.count();
	}

	public Optional<V> get(int tag) {
		return provider.get(tag);
	}

	public Optional<V> remove(int tag) {
		var item = get(tag);
		if (item.isPresent())
			removeValue(item.get());
		return item;
	}

	public void removeValue(V value) {
		provider.remove(value);
	}

	public Stream<V> stream() {
		return provider.stream();
	}
}
