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
package com.sandpolis.core.instance.state;

import com.google.protobuf.Message;

/**
 * A member of the explicit state tree which may be optionally persisted to a
 * datastore.
 *
 * @param <E> A protocol buffer representation of the object
 * @since 5.0.0
 */
public abstract class StateObject<E extends Message> {

	/**
	 * Incorporate the given snapshot into the object. If the snapshot is not
	 * partial, the resulting object
	 *
	 * @param snapshot A state object snapshot
	 */
	public abstract void merge(E snapshot);

	/**
	 * Extract the object's state into a new protocol buffer. If Oids are specified,
	 * the snapshot will be "partial" and only contain descendants of the oids.
	 * 
	 * @param oids Whitelist oids
	 * @return A new protocol buffer representing the object
	 */
	public abstract E snapshot(Oid<?>... oids);
}
