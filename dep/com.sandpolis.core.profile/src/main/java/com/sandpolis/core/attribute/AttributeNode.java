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

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.protobuf.ByteString;
import com.google.protobuf.ByteString.ByteIterator;
import com.sandpolis.core.instance.Updatable.AbstractUpdatable;
import com.sandpolis.core.proto.util.Update.AttributeNodeUpdate;

/**
 * An {@link AttributeNode} represents a node in an attribute tree. Every node
 * (combined with its descendents) in an attribute tree is also a valid subtree
 * and therefore can act as the root of a separate attribute tree.<br>
 * <br>
 * An attribute tree is a bipartite, unidirectional, graph composed of
 * {@link AttributeNode}s. They can have an unlimited number of levels and each
 * node can have a maximum of {@code 2^Integer.SIZE} children.<br>
 * <br>
 * 
 * There are two {@link AttributeNode} implementations: a branch type which may
 * exist anywhere in the tree ({@link AttributeGroup}) and a leaf type
 * ({@link Attribute}) that contains data.<br>
 * <br>
 * The attribute tree can be traversed downwards using the
 * {@link #getNode(ByteIterator)} method with an iterator for an ID chain.<br>
 * <br>
 * The children of a {@link AttributeNode} can be iterated using its
 * {@link #iterator}.
 * 
 * @author cilki
 * @since 5.0.0
 */
@Entity
public abstract class AttributeNode extends AbstractUpdatable<AttributeNodeUpdate> implements Iterable<AttributeNode> {

	/**
	 * Indicates a one-byte characteristic ID.
	 */
	public static final int SINGLE = 1;

	/**
	 * Indicates a two-byte characteristic ID.
	 */
	public static final int DOUBLE = 2;

	/**
	 * Indicates a three-byte characteristic ID.
	 */
	public static final int TRIPLE = 3;

	/**
	 * Indicates a four-byte characteristic ID.
	 */
	public static final int QUAD = 4;

	/**
	 * The database ID.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(nullable = false)
	private int id;

	/**
	 * The node's parent which is stored to allow change events to propegate upwards
	 * in the tree.
	 */
	@Transient // TODO
	private AttributeNode parent;

	/**
	 * Get a descendent of this {@link AttributeNode} according to the given
	 * {@link ByteString} iterator (relative location). Each value from the iterator
	 * specifies which child to search on each level. The implementation will likely
	 * call {@link getNode} recursively on each level until the {@link ByteString}
	 * runs out. When this method is called with an exhausted iterator, it should
	 * return {@code this}.
	 * 
	 * @param key An {@link Iterator} for a {@link ByteString} which fully describes
	 *            the relative location of the desired {@link AttributeNode}
	 * @return The requested {@link AttributeNode} or {@code null}
	 */
	public abstract AttributeNode getNode(ByteIterator key);

	/**
	 * Get a descendent of this {@link AttributeNode} according to the given
	 * {@link ByteString}.
	 * 
	 * @param key A {@link ByteString} which fully describes the location of the
	 *            desired {@link AttributeNode}
	 * @return The requested {@link AttributeNode} or {@code null}
	 */
	public AttributeNode getNode(ByteString key) {
		return getNode(key.iterator());
	}

	/**
	 * Get a descendent of this {@link AttributeNode} according to the given
	 * {@link AttributeNodeKey}. This method cannot be used for plural
	 * {@link AttributeNode}s because {@link AttributeNodeKey}s are strictly static.
	 * 
	 * @param key A {@link AttributeNodeKey} which corresponds to the desired
	 *            {@link AttributeNode}
	 * @return The requested {@link AttributeNode} or {@code null}
	 */
	public AttributeNode getNode(AttributeNodeKey key) {
		return getNode(key.chain().iterator());
	}

	/**
	 * Add a child node. The implementing class must not be a leaf type otherwise
	 * {@link UnsupportedOperationException} will be thrown.
	 * 
	 * @param node A new {@link AttributeNode} which may be a branch or leaf
	 */
	public abstract void addNode(AttributeNode node);

	public void setParent(AttributeNode node) {
		parent = node;
	}

	/**
	 * Get the number of children of this node.
	 * 
	 * @return The number of children
	 */
	public abstract int getSize();

	/**
	 * Get the characteristic ID which uniquely identifies a node among its sibling
	 * nodes.
	 * 
	 * @return The node's characteristic ID
	 */
	public abstract int getCharacteristic();

	/**
	 * Indicates whether this {@link AttributeNode} is anonymous and therefore has
	 * no corresponding {@link AttributeNodeKey}. The parent of an anonymous node
	 * must be plural and all children of a plural node must be anonymous.
	 * 
	 * @return True if this {@link AttributeNode} is anonymous
	 */
	public abstract boolean isAnonymous();

	public Stream<AttributeNode> stream() {
		Iterable<AttributeNode> iterable = () -> iterator();
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	@Transient
	private List<AttributeChangeListener> listeners;

	/**
	 * Register a new listener on this {@link AttributeNode}.
	 * 
	 * @param listener
	 */
	public void register(AttributeChangeListener listener) {
		listeners.add(listener);
	}

	/**
	 * Fire a change event.
	 * 
	 * @param node The node that changed
	 */
	public void fireChanged(AttributeNode node) {
		synchronized (listeners) {
			for (AttributeChangeListener listener : listeners) {
				listener.changed(node);
			}
		}

		if (parent != null)
			parent.fireChanged(node);
	}

	/**
	 * When registered on an {@link AttributeNode}, this listener receives change
	 * notifications for the node and all transitive children of the node.
	 */
	public static interface AttributeChangeListener {

		/**
		 * Indicates an attribute node has changed.
		 * 
		 * @param node The node that changed
		 */
		public void changed(AttributeNode node);

	}
}
