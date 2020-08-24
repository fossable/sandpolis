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
 * A {@link STObject} is any member of the state tree.
 *
 * @param <E> A protocol buffer representation of the object
 * @since 5.0.0
 */
public interface STObject<E extends Message> {

	/**
	 * Incorporate the given snapshot into the object. If the snapshot is not
	 * partial, the object's state becomes identical to the snapshot. If the
	 * snapshot is partial, it has an additive effect.
	 *
	 * @param snapshot A state object snapshot
	 */
	public void merge(E snapshot);

	/**
	 * Extract the object's state into a new protocol buffer. If Oids are specified,
	 * the snapshot will be "partial" and only contain descendants of the oids.
	 * 
	 * @param oids Whitelist oids
	 * @return A new protocol buffer representing the object
	 */
	public E snapshot(Oid<?>... oids);
}
