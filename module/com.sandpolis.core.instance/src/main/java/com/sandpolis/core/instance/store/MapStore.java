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

import com.sandpolis.core.instance.storage.StoreProvider;
import com.sandpolis.core.instance.store.StoreBase.StoreConfig;

public abstract class MapStore<K, V, E extends StoreConfig> extends StoreBase<E> {

	protected StoreProvider<V> provider;

	protected MapStore(Logger log) {
		super(log);
	}

	public Stream<V> stream() {
		return provider.stream();
	}

	public Optional<V> get(K key) {
		return provider.get(key);
	}

	public Optional<V> remove(K key) {
		var item = get(key);
		if (item.isPresent())
			removeValue(item.get());
		return item;
	}

	public void removeValue(V value) {
		provider.remove(value);
	}

	public void add(V item) {
		provider.add(item);
	}

	@Override
	public void close() throws Exception {
		provider = null;
	}
}
