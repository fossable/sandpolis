package com.sandpolis.viewer.lifegem;

import java.util.function.Supplier;

import com.sandpolis.core.instance.State.ProtoAttribute;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.Oid;

import javafx.beans.value.ObservableValueBase;

public class JavaFxAttribute<T> extends ObservableValueBase<T> implements STAttribute<T> {

	private T value;

	/**
	 * An optional supplier that overrides the current value.
	 */
	private Supplier<T> source;

	@Override
	public T getValue() {
		return value;
	}

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
		return value;
	}

	@Override
	public void source(Supplier<T> source) {
		this.source = source;
	}

}
