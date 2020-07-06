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
package com.sandpolis.core.instance.database;

import com.sandpolis.core.instance.store.StoreProvider;

/**
 * Represents the connection to a {@link Database}.
 *
 * @author cilki
 * @since 5.0.0
 */
public abstract class DatabaseConnection implements AutoCloseable {

	/**
	 * Indicated whether the connection is currently open.
	 *
	 * @return The connection status
	 */
	public abstract boolean isOpen();

	/**
	 * Obtain a new {@link StoreProvider} for this database.
	 *
	 * @param cls The class type that the provider will manage
	 * @return A new provider for the given class
	 */
	public abstract <E> StoreProvider<E> provider(Class<E> cls);

}
