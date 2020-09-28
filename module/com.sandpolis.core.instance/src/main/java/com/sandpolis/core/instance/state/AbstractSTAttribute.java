package com.sandpolis.core.instance.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sandpolis.core.instance.State.ProtoAttribute;
import com.sandpolis.core.instance.State.ProtoAttributeValue;
import com.sandpolis.core.instance.state.oid.RelativeOid;

public abstract class AbstractSTAttribute<T> extends AbstractSTObject<ProtoAttribute> implements STAttribute<T> {

	/**
	 * The current value of the attribute.
	 */
	protected STAttributeValue<T> current;

	protected Function<ProtoAttributeValue, T> deserializer;

	/**
	 * Historical values.
	 */
	protected List<STAttributeValue<T>> history;

	protected STDocument parent;

	/**
	 * A strategy that determines what happens to old values.
	 */
	protected RetentionPolicy retention;

	/**
	 * A quantifier for the retention policy.
	 */
	protected long retentionLimit;

	protected Function<T, ProtoAttributeValue.Builder> serializer;

	/**
	 * An optional supplier that overrides the current value.
	 */
	protected Supplier<T> source;

	@Override
	public synchronized T get() {
		if (source != null)
			return source.get();
		if (current == null)
			return null;

		return current.get();
	}

	@Override
	public synchronized List<STAttributeValue<T>> history() {
		if (history == null)
			return List.of();

		return Collections.unmodifiableList(history);
	}

	@Override
	public synchronized void merge(ProtoAttribute snapshot) {
		// Save the old value for inclusion in the event
		var old = current;

		if (snapshot.getValuesList().isEmpty()) {
			current = null;
			if (history != null)
				history.clear();
		} else {

			// Ensure deserializer is loaded
			requireSerializers(snapshot.getTag());

			// Set current value
			current = new EphemeralAttributeValue<>(deserializer.apply(snapshot.getValuesList().get(0)));

			if (history != null) {
				history.clear();

				snapshot.getValuesList().stream().skip(1)
						.map(av -> new EphemeralAttributeValue<>(deserializer.apply(av), av.getTimestamp()))
						.forEach(history::add);
			}
		}
		fireAttributeValueChangedEvent(this, old, current);
	}

	@Override
	public synchronized AbstractSTObject parent() {
		return (AbstractSTObject) parent;
	}

	@Override
	public synchronized void set(T value) {

		// Save the old value for inclusion in the event
		var old = current;

		if (value == null) {
			current = null;

			fireAttributeValueChangedEvent(this, old, null);
			return;
		}

		// If retention is not enabled, then overwrite the old value
		if (retention == null) {
			current = newValue(value);
		}

		// Retention is enabled
		else {
			// Move current value into history
			history.add(current);

			// Set current value
			current = newValue(value);

			// Take action on the old values if necessary
			checkRetention();
		}

		fireAttributeValueChangedEvent(this, old, current);
	}

	public synchronized void setRetention(RetentionPolicy retention) {
		this.retention = retention;
		checkRetention();
	}

	public synchronized void setRetention(RetentionPolicy retention, int limit) {
		this.retention = retention;
		this.retentionLimit = limit;
		checkRetention();
	}

	@Override
	public synchronized ProtoAttribute snapshot(RelativeOid<?>... oids) {
		if (oids.length != 0)
			throw new UnsupportedOperationException("Partial snapshots are not allowed on attributes");

		if (!isPresent())
			// Empty attribute shortcut
			return ProtoAttribute.getDefaultInstance();

		var snapshot = ProtoAttribute.newBuilder();

		// Check the retention condition before serializing
		checkRetention();

		// Ensure serializer is loaded
		requireSerializers(tag);

		// Add current value
		if (source != null) {
			snapshot.addValues(serializer.apply(source.get()).setTimestamp(System.currentTimeMillis()));
		} else {
			snapshot.addValues(serializer.apply(current.get()).setTimestamp(current.timestamp()));
		}

		if (history != null) {
			history.stream().map(av -> serializer.apply(av.get()).setTimestamp(av.timestamp()).build())
					.forEachOrdered(snapshot::addValues);
		}

		return snapshot.build();
	}

	@Override
	public synchronized void source(Supplier<T> source) {
		this.source = source;
	}

	/**
	 * Get the timestamp associated with the current value.
	 *
	 * @return The current timestamp
	 */
	public synchronized long timestamp() {
		if (source != null)
			return 0;
		if (current == null)
			return 0;

		return current.timestamp();
	}

	/**
	 * Check the retention condition and remove all violating elements.
	 */
	private void checkRetention() {
		if (retention == null)
			return;

		if (history == null)
			history = new ArrayList<>();

		switch (retention) {
		case ITEM_LIMITED:
			while (history.size() > retentionLimit) {
				history.remove(0);
			}
			break;
		case TIME_LIMITED:
			while (history.size() > 0 && history.get(0).timestamp() > (current.timestamp() - retentionLimit)) {
				history.remove(0);
			}
			break;
		case UNLIMITED:
			// Do nothing
			break;
		}
	}

	protected void requireSerializers(long tag) {
		if (serializer == null || deserializer == null) {
			var v = STAttributeValue.determine(tag);
			serializer = v[0];
			deserializer = v[1];
		}
	}

	protected abstract STAttributeValue<T> newValue(T value);

	protected abstract STAttributeValue<T> newValue(T value, long timestamp);

	@Override
	public String toString() {
		if (current != null)
			return current.toString();
		return null;
	}
}
