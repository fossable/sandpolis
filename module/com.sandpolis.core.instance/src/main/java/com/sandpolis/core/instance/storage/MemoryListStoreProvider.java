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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * An ephemeral {@link StoreProvider} that is backed by a {@link List}.
 *
 * @author cilki
 * @since 5.0.0
 */
public class MemoryListStoreProvider<K, E> extends EphemeralStoreProvider<E> implements StoreProvider<E> {

	/**
	 * The backing storage for this {@link StoreProvider}.
	 */
	private final List<E> list;

	public MemoryListStoreProvider(Class<E> cls, Function<E, K> keyFunction) {
		this(cls, keyFunction, new ArrayList<>());
	}

	public MemoryListStoreProvider(Class<E> cls, Function<E, K> keyFunction, List<E> list) {
		super(cls);
		this.list = Objects.requireNonNull(list);
	}

	@Override
	public Optional<E> get(Object id) {
		beginStream();
		try {
			for (E e : list) {
				if (id.equals(getId(e)))
					return Optional.of(e);
			}
			return Optional.empty();
		} finally {
			endStream();
		}
	}

	@Override
	public Optional<E> get(String field, Object id) {
		beginStream();
		try {
			MethodHandle getField = fieldGetter(field);
			for (E e : list)
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
