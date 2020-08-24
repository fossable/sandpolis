package com.sandpolis.core.instance.state;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.sandpolis.core.instance.State.ProtoAttribute;

public interface STAttribute<T> extends STObject<ProtoAttribute> {

	public void set(T value);

	public T get();

	public void source(Supplier<T> source);

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
}
