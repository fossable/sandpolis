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
package com.sandpolis.core.instance.state;

import static com.sandpolis.core.instance.state.StateEventStore.StateEventStore;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.sandpolis.core.instance.State.ProtoAttribute;

/**
 * {@link DefaultAttribute} allows attributes to be persistent and optionally
 * saves the history of the attribute's value.
 *
 * @param <T> The type of the attribute's value
 * @since 7.0.0
 */
@Entity
public class DefaultAttribute<T> implements STAttribute<T> {

	@Embeddable
	public enum RetentionPolicy {

		/**
		 * Indicates that changes to the attribute will be retained forever.
		 */
		UNLIMITED,

		/**
		 * Indicates that changes to the attribute will be retained for a fixed period
		 * of time.
		 */
		TIME_LIMITED,

		/**
		 * Indicates that a fixed number of changes to the attribute will be retained.
		 */
		ITEM_LIMITED;
	}

	@Id
	private String db_id;

	/**
	 * The {@link Oid} that corresponds with this attribute.
	 */
	@Column(nullable = false)
	@Convert(converter = OidConverter.class)
	private Oid<T> oid;

	/**
	 * A strategy that determines what happens to old values.
	 */
	@Column(nullable = true)
	private RetentionPolicy retention;

	/**
	 * A quantifier for the retention policy.
	 */
	@Column(nullable = true)
	private long retentionLimit;

	/**
	 * The UTC epoch timestamp associated with the current value.
	 */
	@Column
	private long currentTimestamp;

	/**
	 * The current value of the attribute.
	 */
	@Embedded
	private DefaultAttributeValue<T> current;

	/**
	 * Historical timestamps parallel to {@link #values}.
	 */
	@ElementCollection
	private List<Long> timestamps;

	/**
	 * Historical values parallel to {@link #timestamps}.
	 */
	@ElementCollection
	private List<DefaultAttributeValue<T>> values;

	/**
	 * An optional supplier that overrides the current value.
	 */
	@Transient
	private Supplier<T> source;

	public DefaultAttribute(Oid<T> oid) {
		this.db_id = UUID.randomUUID().toString();
		this.oid = oid;
	}

	protected DefaultAttribute() {
		// JPA Constructor
	}

	@Override
	public synchronized void set(T value) {

		// Save the old value temporarily
		var old = (current == null) ? null : current.get();

		// If the new value is null, clear the entire attribute
		if (value == null) {
			if (current != null)
				// Clear the attribute value, but don't null it
				current.set(null);

			currentTimestamp = 0;
			timestamps.clear();
			values.clear();

			StateEventStore.fire(oid, old, value);
			return;
		}

		// If the current value has not been set, create it (by inefficient means)
		if (current == null) {
			current = DefaultAttributeValue.newAttributeValue(value);
		}

		// If retention is not enabled, then overwrite the old value
		if (retention == null) {
			current.set(value);
			currentTimestamp = System.currentTimeMillis();
		}

		// Retention is enabled
		else {
			// Move current value into history
			values.add(current);
			timestamps.add(currentTimestamp);

			// Set current value
			current = current.clone();
			current.set(value);
			currentTimestamp = System.currentTimeMillis();

			// Take action on the old values if necessary
			checkRetention();
		}

		StateEventStore.fire(oid, old, value);
	}

	@Override
	public synchronized T get() {
		if (source != null)
			return source.get();
		if (current == null)
			return null;

		return current.get();
	}

	/**
	 * Get the timestamp associated with the current value.
	 *
	 * @return The current timestamp or {@code null}
	 */
	public synchronized long timestamp() {
		return currentTimestamp;
	}

	@Override
	public void source(Supplier<T> source) {
		this.source = source;
	}

	/**
	 * Check the retention condition and remove all violating elements.
	 */
	private void checkRetention() {
		if (retention == null)
			return;

		switch (retention) {
		case ITEM_LIMITED:
			while (timestamps.size() > retentionLimit) {
				timestamps.remove(0);
				values.remove(0);
			}
			break;
		case TIME_LIMITED:
			while (timestamps.size() > 0 && timestamps.get(0) > (currentTimestamp - retentionLimit)) {
				timestamps.remove(0);
				values.remove(0);
			}
			break;
		case UNLIMITED:
			// Do nothing
			break;
		}
	}

	public void setRetention(RetentionPolicy retention) {
		this.retention = retention;
		checkRetention();
	}

	public void setRetention(RetentionPolicy retention, int limit) {
		this.retention = retention;
		this.retentionLimit = limit;
		checkRetention();
	}

	@Override
	public synchronized void merge(ProtoAttribute snapshot) {
		var newValues = snapshot.getValuesList();
		if (newValues.isEmpty()) {
			set(null);
		} else {
			current.setProto(newValues.get(0));
			currentTimestamp = newValues.get(0).getTimestamp();

			timestamps.clear();
			values.clear();

			for (int i = 1; i < newValues.size(); i++) {
				var newValue = current.clone();
				newValue.setProto(newValues.get(i));
				values.add(newValue);
				timestamps.add(newValues.get(i).getTimestamp());
			}
		}
	}

	@Override
	public synchronized ProtoAttribute snapshot(Oid<?>... oids) {
		if (oids.length != 0) {
			throw new UnsupportedOperationException("Partial snapshots are not allowed on attributes");
		}

		if (!isPresent())
			return ProtoAttribute.getDefaultInstance();

		// Check the retention condition before serializing
		checkRetention();

		var proto = ProtoAttribute.newBuilder();

		if (source != null) {
			// TODO
		} else {
			proto.addValues(current.getProto().setTimestamp(currentTimestamp));
		}

		for (int i = 0; i < values.size(); i++) {
			proto.addValues(values.get(i).getProto().setTimestamp(timestamps.get(i)));
		}

		return proto.build();
	}
}
