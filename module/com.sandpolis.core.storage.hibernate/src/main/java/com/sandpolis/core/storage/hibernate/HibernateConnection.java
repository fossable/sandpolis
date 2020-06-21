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
		return new HibernateStoreProvider<>(emf, cls);
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
