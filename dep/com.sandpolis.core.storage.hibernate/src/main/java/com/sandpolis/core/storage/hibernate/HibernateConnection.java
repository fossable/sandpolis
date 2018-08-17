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
package com.sandpolis.core.storage.hibernate;

import javax.persistence.EntityManagerFactory;

import com.sandpolis.core.instance.storage.StoreProvider;
import com.sandpolis.core.instance.storage.database.DatabaseConnection;

/**
 * Represents the connection to a Hibernate database.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class HibernateConnection extends DatabaseConnection {

	private EntityManagerFactory emf;

	public HibernateConnection(EntityManagerFactory emf) {
		if (emf == null)
			throw new IllegalArgumentException();

		this.emf = emf;
	}

	@Override
	public boolean isOpen() {
		return emf.isOpen();
	}

	@Override
	public <E> StoreProvider<E> provider(Class<E> cls) {
		return new HibernateStoreProvider<>(cls, emf);
	}

	@Override
	public void close() throws Exception {
		try {
			emf.close();
		} finally {
			emf = null;
		}
	}

}
