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
package com.sandpolis.core.instance;

import com.google.protobuf.GeneratedMessageV3;
import com.sandpolis.core.proto.util.Result.ErrorCode;

/**
 * Indicates that a POJO class has a corresponding protobuf type. The standard
 * name for the protobuf type is {@code "Proto<Implementing Class name>"}.<br>
 * <br>
 * An implementation of this interface must implement a method that converts the
 * object to the protobuf type ({@link #extract()}) and a method that merges the
 * protobuf type into the object ({@link #merge(GeneratedMessageV3)}).
 * 
 * @param <E> The corresponding protobuf type
 * @author cilki
 * @since 5.0.0
 */
public interface ProtoType<E extends GeneratedMessageV3> {

	/**
	 * Incorperate the given protobuf into the object. Any field restrictions are
	 * ignored.<br>
	 * <br>
	 * Implementations should have no side effects if the input was invalid.
	 * 
	 * @param delta The changes
	 * @return An error code if {@code delta} was invalid or {@link ErrorCode#NONE}
	 */
	public ErrorCode merge(E delta);

	/**
	 * Convert the object's entire state to a new protocol buffer.
	 * 
	 * @return A new protobuf fully representing the object
	 */
	public E extract();

}
