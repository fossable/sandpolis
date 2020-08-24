package com.sandpolis.core.instance.state;

import java.util.function.Supplier;

import com.sandpolis.core.instance.State.ProtoAttribute;

public class EphemeralAttribute<T> implements STAttribute<T> {

	private T value;

	/**
	 * An optional supplier that overrides the current value.
	 */
	private Supplier<T> source;

	@Override
	public void merge(ProtoAttribute snapshot) {
		// TODO Auto-generated method stub

	}

	@Override
	public ProtoAttribute snapshot(Oid<?>... oids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void set(T value) {
		this.value = value;
	}

	@Override
	public T get() {
		if (source != null)
			return source.get();

		return value;
	}

	@Override
	public void source(Supplier<T> source) {
		this.source = source;
	}

}
