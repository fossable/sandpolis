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
import java.util.function.Supplier;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import com.sandpolis.core.instance.Attribute.ProtoAttribute;

/**
 * An attribute is a container for data of a specific type and meaning.
 *
 * @param <T> The attribute's value type
 * @author cilki
 * @since 6.2.0
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Attribute<T> implements ProtoType<ProtoAttribute> {

	@Id
	@GeneratedValue(generator = "uuid")
//	@GenericGenerator(name = "uuid", strategy = "uuid2")
	private String db_id;

	@Transient
	private Supplier<T> supplier;

	/**
	 * Set the current value of the attribute.
	 *
	 * @param value The new value to replace the current value or {@code null}
	 */
	public abstract void set(T value);

	/**
	 * Get the current value of the attribute.
	 *
	 * @return The current value or {@code null}
	 */
	public abstract T get();

	/**
	 * Get the timestamp associated with the current value.
	 *
	 * @return The current timestamp or {@code null}
	 */
	public abstract Date timestamp();

	/**
	 * Perform the given operation if the attribute has a current value.
	 *
	 * @param fn A function to receive the current value if it exists
	 */
	public void ifPresent(Consumer<T> fn) {
		var value = get();
		if (value != null)
			fn.accept(value);
	}

	/**
	 * Get whether the attribute has a current value.
	 *
	 * @return Whether the attribute's value is {@code null}
	 */
	public boolean isPresent() {
		return get() != null;
	}

	public void bind(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	/**
	 * An attribute that maintains a timestamped history of its value.
	 *
	 * @param <T> The attribute's value type
	 * @author cilki
	 * @since 6.2.0
	 */
	public static abstract class TrackedAttribute<T> extends Attribute<T> implements List<T> {

		@Override
		public void set(T value) {
			add(value);
		}

		@Override
		public T get() {
			return get(0);
		}

		/**
		 * Clear the history of the attribute.
		 */
		public abstract void clearHistory();
	}
}
