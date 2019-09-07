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

import javax.persistence.Entity;

import com.sandpolis.core.proto.util.Update.AttributeNodeUpdate;
import com.sandpolis.core.proto.util.Update.AttributeUpdate;

/**
 * An {@link Attribute} which only keeps track of its current value and
 * timestamp.
 *
 * @author cilki
 * @since 4.0.0
 */
@Entity
public class UntrackedAttribute<E> extends Attribute<E> {

	/**
	 * Create an empty unassociated UntrackedAttribute.
	 */
	public UntrackedAttribute() {
	}

	/**
	 * Create an unassociated UntrackedAttribute with an initial value.
	 *
	 * @param value The initial value
	 */
	public UntrackedAttribute(E value) {
		set(value);
	}

	/**
	 * Create an unassociated UntrackedAttribute with an initial value and
	 * timestamp.
	 *
	 * @param value The initial value
	 * @param time  An arbitrary initial timestamp
	 */
	public UntrackedAttribute(E value, long time) {
		set(value, time);
	}

	/**
	 * Create an empty associated UntrackedAttribute.
	 *
	 * @param key The AttributeKey associated with this Attribute
	 */
	public UntrackedAttribute(AttributeKey<E> key) {
		this.characteristic = key.getCharacteristic();
	}

	/**
	 * Create an associated UntrackedAttribute with an initial value.
	 *
	 * @param key   The AttributeKey associated with this Attribute
	 * @param value The initial value
	 */
	public UntrackedAttribute(AttributeKey<E> key, E value) {
		this.characteristic = key.getCharacteristic();
		set(value);
	}

	/**
	 * Create an associated UntrackedAttribute with an initial value and timestamp.
	 *
	 * @param key   The AttributeKey associated with this Attribute
	 * @param value The initial value
	 * @param time  An arbitrary initial timestamp
	 */
	public UntrackedAttribute(AttributeKey<E> key, E value, long time) {
		this.characteristic = key.getCharacteristic();
		set(value, time);
	}

	@Override
	public AttributeNodeUpdate getUpdates(long time) {
		if (timestamp < time)
			return null;

		E update = get();
		if (UPDATE_FIELD == null)
			setUpdateField(update);

		return AttributeNodeUpdate.newBuilder()
				.addAttributeUpdate(AttributeUpdate.newBuilder().setField(UPDATE_FIELD, update)).build();
	}

}
