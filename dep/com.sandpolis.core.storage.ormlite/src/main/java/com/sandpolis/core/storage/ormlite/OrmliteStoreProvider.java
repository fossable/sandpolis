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

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.sandpolis.core.instance.storage.StoreProvider;

/**
 * A persistent {@code StoreProvider} that is backed by an Ormlite DAO.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class OrmliteStoreProvider<E> implements StoreProvider<E> {

	private Dao<E, Object> dao;

	public OrmliteStoreProvider(Class<E> cls, ConnectionSource cs) throws SQLException {
		if (cls == null)
			throw new IllegalArgumentException();
		if (cs == null)
			throw new IllegalArgumentException();

		this.dao = DaoManager.createDao(cs, cls);
	}

	@Override
	public void add(E item) {
		try {
			dao.createIfNotExists(item);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
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
	public Iterator<E> iterator() {
		return dao.iterator();
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
	public void remove(E item) {
		try {
			dao.delete(item);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void clear() {
		try {
			TableUtils.clearTable(dao.getConnectionSource(), dao.getDataClass());
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}
