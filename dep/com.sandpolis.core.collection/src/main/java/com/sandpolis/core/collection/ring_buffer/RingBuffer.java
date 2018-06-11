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
package com.sandpolis.core.collection.ring_buffer;

/**
 * A fixed capacity generic ring buffer.
 *
 * @param <E>
 *            The type stored in this buffer.
 */
public class RingBuffer<E> {

	/**
	 * The backing array
	 */
	private final E[] ring;

	/**
	 * The write pointer
	 */
	private int write = 0;

	/**
	 * The number of elements in the buffer
	 */
	private int size = 0;

	/**
	 * Construct a new RingBuffer with fixed capacity.
	 * 
	 * @param capacity
	 *            The maximum number of elements which can be stored at one time.
	 */
	@SuppressWarnings("unchecked")
	public RingBuffer(int capacity) {
		if (capacity <= 0)
			throw new IllegalArgumentException("Invalid capacity: " + capacity);

		ring = (E[]) new Object[capacity];
	}

	/**
	 * Add an element to the buffer
	 * 
	 * @param e
	 *            The element to add
	 */
	public void add(E e) {
		// Store element, possibly overwriting the oldest element
		ring[write] = e;

		// Increase write pointer
		write = (write + 1) % ring.length;

		// Increase size if the ring is not yet full
		if (size < ring.length)
			size++;

	}

	/**
	 * Get an element from the buffer. The element at index 0 is the most recently
	 * added.
	 * 
	 * @param index
	 *            The location of the desired element
	 * @return The element located at index
	 */
	public E get(int index) {
		if (index >= size || index < 0)
			throw new IllegalArgumentException("Invalid index: " + index);
		int pos = write - 1 - index;
		if (pos < 0) {
			pos = size + pos;
		}

		return ring[pos];
	}

	/**
	 * Reset the RingBuffer.
	 */
	public void clear() {
		write = 0;
		size = 0;
	}

	/**
	 * Get the number of elements in the buffer.
	 * 
	 * @return The number of elements currently stored
	 */
	public int size() {
		return size;
	}

	/**
	 * Get the maximum size of the buffer.
	 * 
	 * @return The maximum number of elements which can be stored
	 */
	public int capacity() {
		return ring.length;
	}

}