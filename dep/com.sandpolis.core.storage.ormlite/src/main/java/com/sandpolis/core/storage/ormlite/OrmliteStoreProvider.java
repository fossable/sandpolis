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
package com.sandpolis.core.storage.ormlite;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.sandpolis.core.instance.storage.ConcurrentStoreProvider;
import com.sandpolis.core.instance.storage.StoreProvider;

/**
 * A persistent {@link StoreProvider} that is backed by an Ormlite DAO.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class OrmliteStoreProvider<E> extends ConcurrentStoreProvider<E> implements StoreProvider<E> {

	private Dao<E, Object> dao;

	public OrmliteStoreProvider(Class<E> cls, ConnectionSource cs) throws SQLException {
		if (cls == null)
			throw new IllegalArgumentException();
		if (cs == null)
			throw new IllegalArgumentException();

		this.dao = DaoManager.createDao(cs, cls);
	}

	@Override
	public E get(Object id) {
		try {
			return dao.queryForId(id);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public E get(String field, Object id) {
		try {
			List<E> results = dao.queryForEq(field, id);
			if (results.size() == 0)
				return null;
			if (results.size() > 1)
				return null;// TODO: do something about this case
			return results.get(0);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Stream<E> stream() {
		beginStream();
		CloseableIterator<E> it = dao.iterator();
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.DISTINCT), false)
				.onClose(() -> {
					try {
						it.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					} finally {
						endStream();
					}
				});
	}

	@Override
	public long count() {
		try {
			return dao.countOf();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void add(E item) {
		mutate(() -> dao.createIfNotExists(item));
	}

	@Override
	public void remove(E item) {
		mutate(() -> dao.delete(item));
	}

	@Override
	public void removeIf(Predicate<E> condition) {
		mutate(() -> {
			try (CloseableIterator<E> iterator = dao.iterator()) {
				while (iterator.hasNext())
					if (condition.test(iterator.next()))
						iterator.remove();
			}
		});
	}

	@Override
	public void clear() {
		mutate(() -> TableUtils.clearTable(dao.getConnectionSource(), dao.getDataClass()));
	}

}
