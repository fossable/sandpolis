package com.sandpolis.core.instance.store;

import java.util.Optional;
import java.util.stream.Stream;

import com.sandpolis.core.instance.storage.StoreProvider;

public abstract class MapStore<K, V, E> extends StoreBase<E> {

	protected StoreProvider<V> provider;

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
}
