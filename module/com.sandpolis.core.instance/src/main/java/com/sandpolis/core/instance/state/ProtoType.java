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
 * Indicates that a POJO has a corresponding protobuf representation.
 *
 * @param <E> The protobuf representation of the object
 * @since 5.0.0
 */
public interface ProtoType<E extends Message> {

	/**
	 * Incorperate the given protobuf into the object. Any field restrictions are
	 * ignored.<br>
	 * <br>
	 * Implementations should have no side effects if the input was invalid.
	 *
	 * @param snapshot The changes
	 */
	public void merge(E snapshot);

	/**
	 * Convert the object's entire state to a new protocol buffer.
	 *
	 * @return A new protobuf fully representing the object
	 */
	public E snapshot();

	public E snapshot(Oid<?>... oids);
}
