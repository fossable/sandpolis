/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString.ByteIterator;
import com.sandpolis.core.proto.util.Update.AttributeNodeUpdate;

/**
 * An {@link AttributeGroup} is the secondary constituent of an attribute tree.
 * It can have any number of children (attributes or other attribute groups) and
 * has one parent attribute group (except for the root node).
 * 
 * @author cilki
 * @since 5.0.0
 */
@Entity
public class AttributeGroup extends AttributeNode {

	private static final Logger log = LoggerFactory.getLogger(AttributeGroup.class);

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int db_id;

	/**
	 * The direct children of this {@link AttributeGroup}.
	 */
	@OneToMany(cascade = CascadeType.ALL)
	@MapKeyColumn(name = "db_id")
	private Map<Integer, AttributeNode> children;

	/**
	 * The number of bytes used for the plurality ID. A value of 0 indicates the
	 * {@link AttributeGroup} is singular.
	 */
	@Column
	private int plurality;

	/**
	 * The characteristic ID of this {@link AttributeGroup}.
	 */
	@Column
	private int characteristic;

	/**
	 * Indicates whether this {@link AttributeGroup} is anonymous.
	 */
	@Column
	private boolean anonymous;

	/**
	 * Construct an anonymous {@link AttributeGroup}.
	 */
	public AttributeGroup() {
		this.children = new HashMap<>();
		this.anonymous = true;
	}

	/**
	 * Construct an {@link AttributeGroup} with the corresponding
	 * {@link AttributeGroupKey}.
	 * 
	 * @param key The corresponding {@link AttributeGroupKey}
	 */
	public AttributeGroup(AttributeGroupKey key) {
		this.children = new HashMap<>();
		this.characteristic = key.getCharacteristic();
		this.plurality = key.getPlurality();
		this.anonymous = false;
	}

	@Override
	public AttributeNodeUpdate getUpdates(long time) {
		AttributeNodeUpdate.Builder update = AttributeNodeUpdate.newBuilder();
		for (int characteristic : children.keySet()) {
			AttributeNodeUpdate childUpdate = children.get(characteristic).getUpdates(time);

			if (childUpdate != null)
				update.putAttributeNodeUpdate(characteristic, childUpdate);
		}

		if (update.getAttributeNodeUpdateCount() == 0)
			// No updates
			return null;

		return update.build();
	}

	@Override
	public void merge(AttributeNodeUpdate update) throws Exception {
		for (Entry<Integer, AttributeNodeUpdate> entry : update.getAttributeNodeUpdateMap().entrySet()) {
			int childId = entry.getKey();
			AttributeNodeUpdate childUpdate = entry.getValue();

			if (!children.containsKey(childId)) {
				// Add a new attribute group or attribute
				// TODO currently impossible to know what kind of node should be created
			}

			children.get(childId).merge(childUpdate);
		}
	}

	@Override
	public AttributeNode getNode(ByteIterator chain) {
		if (!chain.hasNext())
			return this;

		int id = readId(chain, plurality == 0 ? 1 : plurality);
		if (children.containsKey(id))
			return children.get(id).getNode(chain);

		return null;
	}

	@Override
	public void addNode(AttributeNode node) {
		if (plurality == 0) {
			assert !node.isAnonymous();
			children.put(node.getCharacteristic(), node);
		} else {
			assert node.isAnonymous();
			children.put(children.size(), node);
		}
	}

	@Override
	public int getSize() {
		return children.size();
	}

	@Override
	public int getCharacteristic() {
		return characteristic;
	}

	@Override
	public boolean isAnonymous() {
		return anonymous;
	}

	@Override
	public Stream<AttributeNode> stream() {
		return children.values().stream();
	}

	/**
	 * Read the next characteristic ID from an ID chain.
	 * 
	 * @param chain The ID chain
	 * @param bytes The number of bytes to read
	 * @return The characteristic ID
	 */
	private static int readId(ByteIterator chain, int bytes) {
		switch (bytes) {
		case SINGLE:
			return chain.next().intValue();
		case DOUBLE:
			return (chain.next().intValue() << 8) + chain.next().intValue();
		case TRIPLE:
			return (chain.next().intValue() << 16) + (chain.next().intValue() << 8) + chain.next().intValue();
		case QUAD:
			return (chain.next().intValue() << 24) + (chain.next().intValue() << 16) + (chain.next().intValue() << 8)
					+ chain.next().intValue();
		default:
			throw new IllegalArgumentException();
		}
	}

}
