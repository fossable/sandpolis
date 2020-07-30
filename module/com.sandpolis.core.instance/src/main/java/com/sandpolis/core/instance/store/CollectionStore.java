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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.sandpolis.core.instance.data.Document;
import com.sandpolis.core.instance.store.provider.StoreProvider;

public abstract class CollectionStore<V, E extends StoreConfig> extends StoreBase<E> {

	protected StoreProvider<V> provider;

	protected CollectionStore(Logger log) {
		super(log);
	}

	public abstract V create(Consumer<V> configurator);

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

	@Override
	public StoreMetadata getMetadata() {
		return provider.getMetadata();
	}
}
