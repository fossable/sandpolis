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
package com.sandpolis.viewer.lifegem.state;

import java.util.function.Supplier;

import com.sandpolis.core.instance.State.ProtoAttribute;
import com.sandpolis.core.instance.state.EphemeralAttribute;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;

import javafx.beans.value.ObservableValueBase;

public class FxAttribute<T> extends ObservableValueBase<T> implements STAttribute<T> {

	private EphemeralAttribute<T> container;

	@Override
	public T getValue() {
		return get();
	}

	@Override
	public void merge(ProtoAttribute snapshot) {
		container.merge(snapshot);
	}

	@Override
	public ProtoAttribute snapshot(RelativeOid<?>... oids) {
		return container.snapshot(oids);
	}

	@Override
	public void set(T value) {
		container.set(value);
	}

	@Override
	public T get() {
		return container.get();
	}

	@Override
	public void source(Supplier<T> source) {
		container.source(source);
	}

	@Override
	public STCollection.EventListener addListener(STCollection.EventListener listener) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> STAttribute.EventListener<T> addListener(STAttribute.EventListener<T> listener) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeListener(Object listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public Oid oid() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setOid(Oid oid) {
		// TODO Auto-generated method stub

	}

}
