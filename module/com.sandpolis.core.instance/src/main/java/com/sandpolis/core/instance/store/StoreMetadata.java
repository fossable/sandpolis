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

/**
 * {@link StoreMetadata} contains information about the store itself.
 */
public interface StoreMetadata {

	/**
	 * Get the number of times the store has been initialized.
	 *
	 * @return The initialization count
	 */
	public int getInitCount();
}
