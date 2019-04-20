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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;

import com.google.common.collect.Lists;
import com.sandpolis.core.proto.util.Update.AttributeNodeUpdate;
import com.sandpolis.core.proto.util.Update.AttributeUpdate;

/**
 * A TrackedAttribute stores the timestamped history of its value. Be careful
 * with this attribute because it can quickly become very large if its value is
 * updated frequently.
 * 
 * @author cilki
 * @since 4.0.0
 */
@Entity
public class TrackedAttribute<E> extends Attribute<E> {

	/**
	 * The Attribute history. New entries are added to the end, so the oldest
	 * element will always be the first element.
	 */
	private List<UntrackedAttribute<E>> history;

	/**
	 * Create an empty TrackedAttribute.
	 */
	public TrackedAttribute() {
		this.history = new ArrayList<>();
	}

	/**
	 * Create a new TrackedAttribute.
	 * 
	 * @param key The AttributeKey associated with this Attribute
	 */
	public TrackedAttribute(AttributeKey<E> key) {
		this();
		this.characteristic = key.getCharacteristic();
	}

	/**
	 * Get the history size.
	 * 
	 * @return The number of entries in the attribute history
	 */
	public int size() {
		return history.size();
	}

	/**
	 * Clear this Attribute's history.
	 */
	public void clearHistory() {
		history.clear();
	}

	/**
	 * Get a value from the history.
	 * 
	 * @param index The index into the history where 0 is the oldest entry
	 * @return The specified timestamp in the history
	 */
	public E getValue(int index) {
		return history.get(index).get();
	}

	/**
	 * Get a timestamp from the history.
	 * 
	 * @param index The index into the history where 0 is the oldest entry
	 * @return The specified timestamp in the history
	 */
	public long getTime(int index) {
		return history.get(index).getTimestamp();
	}

	@Override
	public void set(E value) {
		set(value, System.currentTimeMillis());
	}

	@Override
	public void set(E value, long time) {
		if (current != null)
			history.add(new UntrackedAttribute<E>(current, timestamp)); // TODO insert by timestamp

		super.set(value, time);
	}

	@Override
	public AttributeNodeUpdate getUpdates(long time) {
		if (timestamp < time)
			return null;

		if (UPDATE_FIELD == null)
			setUpdateField(get());

		AttributeNodeUpdate.Builder nodeUpdate = AttributeNodeUpdate.newBuilder();
		for (UntrackedAttribute<E> item : Lists.reverse(history)) {
			if (timestamp > item.getTimestamp())
				break;

			nodeUpdate.addAttributeUpdate(AttributeUpdate.newBuilder().setField(UPDATE_FIELD, item.get()));
		}

		return nodeUpdate.build();
	}

}
