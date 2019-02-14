/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
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
package com.sandpolis.core.attribute;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import com.google.protobuf.ByteString;

/**
 * Corresponds to an {@link AttributeGroup}.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class AttributeGroupKey extends AttributeNodeKey {

	/**
	 * The number of bytes used in the corresponding {@link AttributeGroup} for the
	 * plurality ID. A value of 0 indicates the group is singular.
	 */
	private int plurality;

	/**
	 * Build an {@link AttributeGroupKey} that corresponds to a singular
	 * {@link AttributeGroup}.
	 * 
	 * @param parent         The parent key
	 * @param characteristic The characteristic ID
	 */
	public AttributeGroupKey(AttributeNodeKey parent, int characteristic) {
		this(parent, characteristic, 0);
	}

	/**
	 * Build an {@link AttributeGroupKey} that corresponds to a plural
	 * {@link AttributeGroup}.
	 * 
	 * @param parent         The parent key
	 * @param characteristic The characteristic ID
	 * @param plurality      The plurality size
	 */
	public AttributeGroupKey(AttributeNodeKey parent, int characteristic, int plurality) {
		checkArgument(plurality >= 1);
		checkArgument(plurality <= 4);

		this.parent = Objects.requireNonNull(parent);
		this.plurality = plurality;
		this.characteristic = characteristic;
		this.key = parent.key.concat(ByteString.copyFrom(new byte[plurality]))
				.concat(ByteString.copyFrom(new byte[] { (byte) characteristic }));
	}

	protected AttributeGroupKey() {
	}

	/**
	 * Get the number of bytes allocated to the plural ID. A value of 0 implies the
	 * corresponding node is not plural.
	 * 
	 * @return The corresponding node's plurality number
	 */
	public int getPlurality() {
		return plurality;
	}
}
