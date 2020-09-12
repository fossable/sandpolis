package com.sandpolis.core.instance.state;

import com.sandpolis.core.instance.State.ProtoAttributeValue;

public interface STAttributeValue<T> {

	/**
	 * Get the value.
	 *
	 * @return The value
	 */
	public T get();

	/**
	 * Set the value.
	 *
	 * @param value The new value
	 */
	public void set(T value);

	/**
	 * Get the value's timestamp.
	 *
	 * @return The timestamp
	 */
	public long getTimestamp();

	/**
	 * Set the value's timestamp.
	 *
	 * @param value The new timestamp
	 */
	public void setTimestamp(long timestamp);

	/**
	 * Get the value as a {@link ProtoAttributeValue}.
	 *
	 * @return The value
	 */
	public ProtoAttributeValue toProto();

	/**
	 * Set the value from a {@link ProtoAttributeValue}.
	 *
	 * @param av The new value
	 */
	public void fromProto(ProtoAttributeValue av);
}
