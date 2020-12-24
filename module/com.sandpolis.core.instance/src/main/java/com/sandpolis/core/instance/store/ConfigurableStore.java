//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.instance.store;

import java.util.function.Consumer;

/**
 * {@link ConfigurableStore} is a store that requires initialization by
 * consumers before it can be used. Initialization is idempotent and may happen
 * more than once.
 *
 * @param <E> The configuration type
 */
public interface ConfigurableStore<E> {

	/**
	 * Initialize the store with the given configurator.
	 *
	 * @param configurator The initialization block
	 */
	public void init(Consumer<E> configurator);

//	public boolean isInitialized();
}
