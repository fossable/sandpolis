/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
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
