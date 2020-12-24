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
package com.sandpolis.core.instance.state.st;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.persistence.Embeddable;

import com.sandpolis.core.instance.State.ProtoAttribute;

/**
 * {@link STAttribute} is a generic container for data of a specific type and
 * meaning.
 *
 * @param <T> The type of the attribute's value
 * @since 6.2.0
 */
public interface STAttribute<T> extends STObject<ProtoAttribute> {

	/**
	 * Indicates that an {@link STAttribute}'s value has changed.
	 *
	 * @param <T> The attribute value's type
	 */
	public static final class ChangeEvent<T> {
		public final STAttribute<T> attribute;
		public final STAttributeValue<T> newValue;
		public final STAttributeValue<T> oldValue;

		public ChangeEvent(STAttribute<T> attribute, STAttributeValue<T> oldValue, STAttributeValue<T> newValue) {
			this.attribute = attribute;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}
	}

	@Embeddable
	public enum RetentionPolicy {

		/**
		 * Indicates that a fixed number of changes to the attribute will be retained.
		 */
		ITEM_LIMITED,

		/**
		 * Indicates that changes to the attribute will be retained for a fixed period
		 * of time.
		 */
		TIME_LIMITED,

		/**
		 * Indicates that changes to the attribute will be retained forever.
		 */
		UNLIMITED;
	}

	/**
	 * Get the current value of the attribute.
	 *
	 * @return The current value or {@code null} if there's no value
	 */
	public T get();

	/**
	 * Get the history of the attribute's value if enabled by the
	 * {@link RetentionPolicy}.
	 *
	 * @return An unmodifiable list
	 */
	public List<STAttributeValue<T>> history();

	/**
	 * Perform the given operation if the attribute has a current value.
	 *
	 * @param fn A function to receive the current value if it exists
	 */
	public default void ifPresent(Consumer<T> fn) {
		var value = get();
		if (value != null)
			fn.accept(value);
	}

	/**
	 * Get whether the attribute has a current value.
	 *
	 * @return Whether the attribute's value is {@code null}
	 */
	public default boolean isPresent() {
		return get() != null;
	}

	/**
	 * Set the current value of the attribute.
	 *
	 * @param value The new value to replace the current value or {@code null}
	 */
	public void set(T value);

	/**
	 * Specify a source for the attribute's value. Setting an attribute source
	 * "binds" the attribute and will cause {@link #set(Object)} calls to fail.
	 *
	 * @param source The source or {@code null} to remove the previous source
	 */
	public void source(Supplier<T> source);

	/**
	 * Get the timestamp associated with the attribute's current value.
	 *
	 * @return The current timestamp
	 */
	public long timestamp();
}
