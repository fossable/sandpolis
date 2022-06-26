//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.state.st;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.s7s.core.foundation.Platform.OsType;
import org.s7s.core.foundation.Instance.InstanceFlavor;
import org.s7s.core.foundation.Instance.InstanceType;
import org.s7s.core.instance.state.st.EphemeralAttribute.EphemeralAttributeValue;

/**
 * {@link STAttribute} is a generic container for data of a specific type and
 * meaning.
 *
 * @param <T> The type of the attribute's value
 * @since 6.2.0
 */
public interface STAttribute extends STObject {

	/**
	 * Indicates that an {@link STAttribute}'s value has changed.
	 */
	public static final record ChangeEvent(STAttribute attribute, EphemeralAttributeValue newValue,
			EphemeralAttributeValue oldValue) {
	}

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

	public default boolean asBoolean(boolean... _default) {
		var value = get();
		if (value == null) {
			if (_default.length > 0) {
				return _default[0];
			}
			throw new NoSuchElementException("No value present");
		}

		if (value instanceof Boolean v) {
			return v;
		}
		if (value instanceof String v) {
			return Boolean.parseBoolean(v);
		}

		throw new ClassCastException(value.getClass().getName());
	}

	public default byte[] asBytes() {
		var value = get();
		if (value == null)
			throw new NoSuchElementException("No value present");

		if (value instanceof byte[] v) {
			return v;
		}
		if (value instanceof String v) {
			return v.getBytes();
		}

		throw new ClassCastException(value.getClass().getName());
	}

	public default InstanceFlavor asInstanceFlavor() {
		var value = get();
		if (value == null)
			throw new NoSuchElementException("No value present");

		if (value instanceof InstanceFlavor v) {
			return v;
		}

		throw new ClassCastException(value.getClass().getName());
	}

	public default InstanceType asInstanceType() {
		var value = get();
		if (value == null)
			throw new NoSuchElementException("No value present");

		if (value instanceof InstanceType v) {
			return v;
		}

		throw new ClassCastException(value.getClass().getName());
	}

	public default OsType asOsType() {
		var value = get();
		if (value == null)
			throw new NoSuchElementException("No value present");

		if (value instanceof OsType v) {
			return v;
		}

		throw new ClassCastException(value.getClass().getName());
	}

	public default int asInt() {
		var value = get();
		if (value == null)
			throw new NoSuchElementException("No value present");

		if (value instanceof Integer v) {
			return v;
		}
		if (value instanceof String v) {
			return Integer.parseInt(v);
		}

		throw new ClassCastException(value.getClass().getName());
	}

	public default int[] asIntArray() {
		var value = get();
		if (value == null)
			throw new NoSuchElementException("No value present");

		if (value instanceof int[] v) {
			return v;
		}

		throw new ClassCastException(value.getClass().getName());
	}

	public default long asLong() {
		var value = get();
		if (value == null)
			throw new NoSuchElementException("No value present");

		if (value instanceof Long v) {
			return v;
		}
		if (value instanceof String v) {
			return Long.parseLong(v);
		}

		throw new ClassCastException(value.getClass().getName());
	}

	public default String asString() {
		var value = get();
		if (value == null)
			throw new NoSuchElementException("No value present");

		if (value instanceof String v) {
			return v;
		}
		if (value instanceof Integer v) {
			return Integer.toString(v);
		}

		throw new ClassCastException(value.getClass().getName());
	}

	public default X509Certificate asX590Certificate() {
		var value = get();
		if (value == null)
			throw new NoSuchElementException("No value present");

		if (value instanceof X509Certificate v) {
			return v;
		}

		throw new ClassCastException(value.getClass().getName());
	}

	public Object get();

	/**
	 * Get the history of the attribute's value if enabled by the
	 * {@link RetentionPolicy}.
	 *
	 * @return An unmodifiable list
	 */
	public List<EphemeralAttributeValue> history();

	public default <E> void ifPresent(Consumer<E> consumer) {
		var value = get();
		if (value != null) {
			consumer.accept((E) value);
		}
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
	public void set(Object value);

	/**
	 * Specify a source for the attribute's value. Setting an attribute source
	 * "binds" the attribute and will cause {@link #set(Object)} calls to fail.
	 *
	 * @param source The source or {@code null} to remove the previous source
	 */
	public void source(Supplier<?> source);

	/**
	 * Get the timestamp associated with the attribute's current value.
	 *
	 * @return The current timestamp
	 */
	public long timestamp();
}
