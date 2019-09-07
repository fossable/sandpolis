/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.core.attribute;

import java.util.HashMap;
import java.util.Map;

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
 * @author cilki
 * @since 5.0.0
 */
public abstract class AttributeNodeKey {

	/**
	 * A chain of bytes that uniquely identifies the {@link AttributeNode} in the
	 * tree that corresponds with this {@link AttributeNodeKey}.
	 */
	protected ByteString key;

	/**
	 * The characteristic identifier which uniquely identifies a node among its
	 * sibling nodes.
	 */
	protected int characteristic;

	/**
	 * This {@link AttributeNodeKey}'s parent.
	 */
	protected AttributeNodeKey parent;

	/**
	 * A map of auxiliary objects related to this key.
	 */
	private Map<String, Object> aux = new HashMap<>();

	/**
	 * Get whether the given id has an associated auxiliary object for {@code this}.
	 *
	 * @param id The auxiliary object id
	 * @return Whether {@code this} has an object associated with id
	 */
	public boolean containsObject(String id) {
		return aux.containsKey(id);
	}

	/**
	 * Get the auxiliary object associated with the given id.
	 *
	 * @param id The auxiliary object id
	 * @return The requested auxiliary object
	 */
	@SuppressWarnings("unchecked")
	public <T> T getObject(String id) {
		return (T) aux.get(id);
	}

	/**
	 * Associate the given auxiliary object with the given id.
	 *
	 * @param id    The auxiliary object id
	 * @param value The new object
	 */
	public void putObject(String id, Object value) {
		aux.put(id, value);
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
	 * Get the characteristic ID which uniquely identifies a node among its sibling
	 * nodes.
	 *
	 * @return The corresponding node's characteristic ID
	 */
	public int getCharacteristic() {
		return characteristic;
	}

	/**
	 * Get the key's domain recursively.
	 *
	 * @return The {@link AttributeNodeKey}'s domain
	 */
	public String getDomain() {
		if (parent == null)
			return ((AttributeDomainKey) this).getDomain();

		return parent.getDomain();
	}

	/**
	 * Check if the given key is an ancestor or equal to {@code this}.
	 *
	 * @param key The key
	 * @return Whether the key is an ancestor or equal to {@code this}
	 */
	public boolean isAncestor(AttributeNodeKey key) {
		if (this.equals(key))
			return true;
		if (parent == null)
			return false;
		return parent.isAncestor(key);
	}

}
