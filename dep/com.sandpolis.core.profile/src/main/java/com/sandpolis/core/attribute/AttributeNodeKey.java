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
package com.sandpolis.core.attribute;

import com.google.protobuf.ByteString;

/**
 * An {@link AttributeNodeKey} corresponds to an {@link AttributeNode} in an
 * attribute tree. All attribute keys are static and form their own tree
 * structure called the attribute-key tree. Every attribute tree mirrors the
 * attribute-key tree. This setup makes it easy to access attributes located on
 * complex paths in the attribute tree because every node of the attribute-key
 * tree is public, static, and final.<br>
 * <br>
 * Although every key has a corresponding node in the attribute tree, the
 * reverse is not true. Nodes that do not have a corresponding key in the
 * attribute-key tree are called anonymous nodes.
 * 
 * @see AttributeKey
 * @author cilki
 * @since 5.0.0
 */
public class AttributeNodeKey {

	/**
	 * Corresponds to the root of an attribute tree.
	 */
	public static final AttributeNodeKey ROOT = new AttributeNodeKey();

	/**
	 * A chain of bytes that uniquely identifies the {@link AttributeNode} in the
	 * tree that corresponds with this {@link AttributeNodeKey}.
	 */
	private ByteString key;

	/**
	 * The number of bytes used in the corresponding {@link AttributeGroup} for the
	 * plurality ID. A value of 0 indicates the attribute group is singular.<br>
	 * <br>
	 * Plurality has no meaning for {@link Attribute}s because they cannot have
	 * descendents.
	 */
	private int plurality;

	/**
	 * The characteristic identifier which uniquely identifies a node among its
	 * sibling nodes.
	 */
	private int characteristic;

	/**
	 * Construct the root of an attribute tree.
	 */
	private AttributeNodeKey() {
		key = ByteString.EMPTY;
	}

	/**
	 * Construct a singular top-level key.
	 * 
	 * @param characteristic
	 *            The single-byte characteristic ID
	 */
	public AttributeNodeKey(int characteristic) {
		this(characteristic, 0);
	}

	/**
	 * Construct a singular key with the given parent.
	 * 
	 * @param parent
	 *            The parent key
	 * @param characteristic
	 *            The single-byte characteristic ID
	 */
	public AttributeNodeKey(AttributeNodeKey parent, int characteristic) {
		this(parent, characteristic, 0);
	}

	/**
	 * Construct a plural top-level key.
	 * 
	 * @param characteristic
	 *            The single-byte characteristic ID
	 * @param pluralWidth
	 *            The number of bytes for the plurality field
	 */
	public AttributeNodeKey(int characteristic, int pluralWidth) {
		this(ROOT, characteristic, pluralWidth);
	}

	/**
	 * Construct a plural key with the given parent.
	 * 
	 * @param parent
	 *            The parent key
	 * @param characteristic
	 *            The single-byte characteristic ID
	 * @param pluralWidth
	 *            The number of bytes for the plurality field
	 */
	public AttributeNodeKey(AttributeNodeKey parent, int characteristic, int pluralWidth) {
		if (parent == null)
			throw new IllegalArgumentException();
		if (pluralWidth < 0 || pluralWidth > 4)
			throw new IllegalArgumentException();

		this.plurality = pluralWidth;
		this.characteristic = characteristic;

		key = parent.key;

		if (parent.getPlurality() > 0)
			key = key.concat(ByteString.copyFrom(new byte[parent.getPlurality()]));

		key = key.concat(ByteString.copyFrom(new byte[] { (byte) characteristic }));
	}

	/**
	 * Get the {@link AttributeNode} identifier.
	 * 
	 * @return The {@link AttributeNode} identifier
	 */
	public ByteString chain() {
		return key;
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

	/**
	 * Get the characteristic ID which uniquely identifies a node among its sibling
	 * nodes.
	 * 
	 * @return The corresponding node's characteristic ID
	 */
	public int getCharacteristic() {
		return characteristic;
	}

}
