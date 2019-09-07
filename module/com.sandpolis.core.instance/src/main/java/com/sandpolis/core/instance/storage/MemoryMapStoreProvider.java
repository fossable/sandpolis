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
package com.sandpolis.core.instance.storage;

import java.lang.invoke.MethodHandle;
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
public class MemoryMapStoreProvider<K, E> extends EphemeralStoreProvider<E> implements StoreProvider<E> {

	/**
	 * The backing storage for this {@code StoreProvider}.
	 */
	private final Map<Object, E> map;

	private final Function<E, K> keyFunction;

	public MemoryMapStoreProvider(Class<E> cls, Function<E, K> keyFunction) {
		this(cls, keyFunction, new HashMap<>());
	}

	public MemoryMapStoreProvider(Class<E> cls, Function<E, K> keyFunction, Map<Object, E> map) {
		super(cls);
		this.map = Objects.requireNonNull(map);
		this.keyFunction = keyFunction;
	}

	@Override
	public Optional<E> get(Object id) {
		return Optional.ofNullable(map.get(id));
	}

	@Override
	public Optional<E> get(String field, Object id) {
		beginStream();
		try {
			MethodHandle getField = fieldGetter(field);
			for (E e : map.values())
				if (id.equals(getField.invoke(e)))
					return Optional.of(e);
			return Optional.empty();
		} catch (Throwable t) {
			throw new RuntimeException(t);
		} finally {
			endStream();
		}
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
