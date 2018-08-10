/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.core.instance.storage;

import java.lang.invoke.MethodHandle;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * An ephemeral {@link StoreProvider} that is backed by a {@link LinkedList}.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class MemoryListStoreProvider<E> extends EphemeralStoreProvider<E> implements StoreProvider<E> {

	/**
	 * The backing storage for this {@link StoreProvider}.
	 */
	private final List<E> list;

	public MemoryListStoreProvider(Class<E> cls) {
		super(cls);
		this.list = new LinkedList<>();
	}

	@Override
	public void add(E e) {
		list.add(e);
	}

	@Override
	public E get(Object id) {
		for (E e : list) {
			if (getId(e).equals(id))
				return e;
		}
		return null;
	}

	@Override
	public E get(String field, Object id) {
		try {
			MethodHandle getField = fieldGetter(field);
			for (E e : list)
				if (id.equals(getField.invoke(e)))
					return e;
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
		return null;
	}

	@Override
	public Iterator<E> iterator() {
		return list.iterator();
	}

	@Override
	public Stream<E> stream() {
		return list.stream();
	}

	@Override
	public void remove(E e) {
		list.remove(e);
	}

}
