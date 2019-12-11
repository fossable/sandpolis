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
package com.sandpolis.core.instance;

import com.google.protobuf.Message;
import com.sandpolis.core.proto.util.Result.ErrorCode;

/**
 * Indicates that a POJO class has a corresponding protobuf type. The standard
 * name for the protobuf type is {@code "Proto<Implementing Class name>"}.<br>
 * <br>
 * An implementation of this interface must implement a method that converts the
 * object to the protobuf type ({@link #extract()}) and a method that merges the
 * protobuf type into the object ({@link #merge(Message)}).
 *
 * @param <E> The corresponding protobuf type
 * @author cilki
 * @since 5.0.0
 */
public interface ProtoType<E extends Message> {

	/**
	 * Incorperate the given protobuf into the object. Any field restrictions are
	 * ignored.<br>
	 * <br>
	 * Implementations should have no side effects if the input was invalid.
	 *
	 * @param delta The changes
	 * @return An error code if {@code delta} was invalid or {@link ErrorCode#OK}
	 */
	public ErrorCode merge(E delta) throws Exception;

	/**
	 * Convert the object's entire state to a new protocol buffer.
	 *
	 * @return A new protobuf fully representing the object
	 */
	public E extract();

	public default E delta(long timestamp) {
		throw new UnsupportedOperationException();
	}

}
