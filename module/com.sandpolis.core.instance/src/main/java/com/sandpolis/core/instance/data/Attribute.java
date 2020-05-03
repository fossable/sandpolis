//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.core.instance.data;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import com.sandpolis.core.instance.Attribute.ProtoAttribute;

/**
 * An attribute is a container for data of a specific type and meaning.
 *
 * @param <T> The attribute's value type
 * @author cilki
 * @since 6.2.0
 */
public interface Attribute<T> extends ProtoType<ProtoAttribute> {

	/**
	 * Set the current value of the attribute.
	 *
	 * @param value The new value to replace the current value or {@code null}
	 */
	void set(T value);

	/**
	 * Get the current value of the attribute.
	 *
	 * @return The current value or {@code null}
	 */
	T get();

	/**
	 * Get the timestamp associated with the current value.
	 *
	 * @return The current timestamp or {@code null}
	 */
	Date timestamp();

	/**
	 * Perform the given operation if the attribute has a current value.
	 *
	 * @param fn A function to receive the current value if it exists
	 */
	default void ifPresent(Consumer<T> fn) {
		var value = get();
		if (value != null)
			fn.accept(value);
	}

	/**
	 * Get whether the attribute has a current value.
	 *
	 * @return Whether the attribute's value is {@code null}
	 */
	default boolean isPresent() {
		return get() != null;
	}

	/**
	 * An attribute that maintains a timestamped history of its value.
	 *
	 * @param <T> The attribute's value type
	 * @author cilki
	 * @since 6.2.0
	 */
	public static interface TrackedAttribute<T> extends Attribute<T>, List<T> {

		@Override
		default void set(T value) {
			add(value);
		}

		@Override
		default T get() {
			return get(0);
		}

		/**
		 * Clear the history of the attribute.
		 */
		void clearHistory();
	}
}
