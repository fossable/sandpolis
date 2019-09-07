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
package com.sandpolis.core.instance;

import javax.persistence.MappedSuperclass;

import com.google.protobuf.Message;

/**
 * Provides a consistent API for merging and extracting protobuf updates.
 *
 * @param <E> The protobuf type that is considered an update.
 * @author cilki
 * @since 5.0.0
 */
public interface Updatable<E extends Message> {

	/**
	 * Merge updates into this {@link Updatable}.
	 *
	 * @param updates The object's updates
	 */
	public void merge(E updates) throws Exception;

	/**
	 * Get updates since (but not including) the given timestamp.
	 *
	 * @param time The lower-bound timestamp
	 * @return Every update since the timestamp
	 */
	public E getUpdates(long time);

	/**
	 * Get all possible updates.
	 *
	 * @return Every update since the object's creation
	 */
	public E getUpdates();

	/**
	 * Get the last modification timestamp.
	 *
	 * @return The timestamp of the current value
	 */
	public long getTimestamp();

	/**
	 * Indicates that a protobuf update is of the wrong type or invalid in some
	 * other way.
	 */
	public static class InvalidUpdateException extends Exception {
	}

	/**
	 * An abstract implementation of {@link Updatable} than handles timestamps.
	 */
	@MappedSuperclass
	public abstract class AbstractUpdatable<E extends Message> implements Updatable<E> {

		/**
		 * A timestamp of the most recent update.
		 */
		protected long timestamp;

		protected void updated() {
			timestamp = System.currentTimeMillis();
		}

		@Override
		public E getUpdates() {
			return getUpdates(0);
		}

		@Override
		public long getTimestamp() {
			return timestamp;
		}
	}
}
