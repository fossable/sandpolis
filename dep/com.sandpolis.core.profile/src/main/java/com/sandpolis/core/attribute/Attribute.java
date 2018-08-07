/******************************************************************************
 *                                                                            *
 *                    Copyright 2016 Subterranean Security                    *
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

import java.util.Collections;
import java.util.Iterator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.MetaValue;

import com.google.protobuf.ByteString.ByteIterator;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.sandpolis.core.proto.util.Update.AttributeNodeUpdate;
import com.sandpolis.core.proto.util.Update.AttributeUpdate;

/**
 * An {@link Attribute} is the primary constituent of an attribute tree. It
 * contains a datum of one of the following types:
 * <li>String</li>
 * <li>Integer</li>
 * <li>Long</li>
 * <li>Double</li>
 * <li>Boolean</li>
 * 
 * @param <E> The type the {@link Attribute} contains
 * @see UntrackedAttribute
 * @see TrackedAttribute
 * @author cilki
 * @since 4.0.0
 */
@Entity
public abstract class Attribute<E> extends AttributeNode {

	/**
	 * The descriptor for the {@link AttributeUpdate} oneof which will contain an
	 * updated value. This exists as a static field to save a few method calls
	 * during a {@link #merge}.
	 */
	protected static final OneofDescriptor UPDATE_ONEOF = AttributeUpdate.getDescriptor().getOneofs().get(0);

	/**
	 * The current value of the {@link Attribute}.
	 */
	@Any(metaColumn = @Column(name = "attribute_type"))
	@Cascade(CascadeType.ALL)
	@AnyMetaDef(idType = "integer", metaType = "string", metaValues = {
			@MetaValue(value = "Integer", targetEntity = Integer.class),
			@MetaValue(value = "Long", targetEntity = Long.class),
			@MetaValue(value = "Double", targetEntity = Double.class),
			@MetaValue(value = "Boolean", targetEntity = Boolean.class),
			@MetaValue(value = "String", targetEntity = String.class) })
	@JoinColumn(name = "property_id")
	protected E current;

	/**
	 * The characteristic ID of the {@link Attribute}.
	 */
	@Column
	protected int characteristic;

	/**
	 * Get the current value of this {@link Attribute}.
	 * 
	 * @return The current value
	 */
	public E get() {
		return current;
	}

	/**
	 * Set the current value of this {@link Attribute}.
	 * 
	 * @param value The new value
	 */
	public void set(E value) {
		set(value, System.currentTimeMillis());
	}

	/**
	 * Set the current value of this {@link Attribute} with an arbitrary timestamp.
	 * 
	 * @param value The new value
	 * @param time  The new value's timestamp
	 */
	public void set(E value, long time) {
		current = value;
		timestamp = time;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void merge(AttributeNodeUpdate updates) throws Exception {
		for (AttributeUpdate update : updates.getAttributeUpdateList()) {
			FieldDescriptor oneof = update.getOneofFieldDescriptor(UPDATE_ONEOF);
			if (oneof == null)
				throw new InvalidUpdateException();

			set((E) update.getField(oneof));
		}
	}

	@Override
	public AttributeNode getNode(ByteIterator key) {
		assert !key.hasNext();
		return this;
	}

	@Override
	public void addNode(AttributeNode node) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getSize() {
		return 0;
	}

	@Override
	public boolean isAnonymous() {
		return false;
	}

	@Override
	public int getCharacteristic() {
		return characteristic;
	}

	@Override
	public Iterator<AttributeNode> iterator() {
		return Collections.emptyIterator();
	}

	@Override
	public String toString() {
		if (current == null)
			return "null";
		return current.toString();
	}

}
