package com.sandpolis.core.instance.store;

import java.util.Optional;
import java.util.stream.Stream;

import com.sandpolis.core.instance.storage.StoreProvider;

public abstract class MapStore<K, V, E> extends StoreBase<E> {

	protected StoreProvider<V> provider;

	public Stream<V> stream() {
		return null;
	}

	public Optional<V> get(K key) {
		return null;
	}

	public Optional<V> remove(K key) {
		return null;
	}

	public boolean exists(K key) {
		return false;
	}
}
