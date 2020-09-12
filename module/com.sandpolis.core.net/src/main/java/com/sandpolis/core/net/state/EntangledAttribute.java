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
package com.sandpolis.core.net.state;

import java.util.Objects;
import java.util.function.Supplier;

import com.sandpolis.core.instance.State.ProtoAttribute;
import com.sandpolis.core.instance.state.AbstractSTObject;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.net.state.STCmd.STSyncStruct;

public class EntangledAttribute<T> extends EntangledObject<ProtoAttribute> implements STAttribute<T> {

	private STAttribute<T> container;

	public EntangledAttribute(STAttribute<T> container, STSyncStruct config) {
		this.container = Objects.requireNonNull(container);

		// Start streams
		switch (config.direction) {
		case BIDIRECTIONAL:
			startSource(config);
			startSink(config, ProtoAttribute.class);
			break;
		case DOWNSTREAM:
			if (config.initiator) {
				startSink(config, ProtoAttribute.class);
			} else {
				startSource(config);
			}
			break;
		case UPSTREAM:
			if (config.initiator) {
				startSource(config);
			} else {
				startSink(config, ProtoAttribute.class);
			}
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	// Begin boilerplate

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
	public void addListener(Object listener) {
		container.addListener(listener);
	}

	@Override
	public void removeListener(Object listener) {
		container.removeListener(listener);
	}

	@Override
	public Oid oid() {
		return container.oid();
	}

	@Override
	public int getTag() {
		return ((AbstractSTObject) container).getTag();
	}

	@Override
	public void setTag(int tag) {
		container.setTag(tag);
	}

	@Override
	public AbstractSTObject parent() {
		return ((AbstractSTObject) container).parent();
	}

	@Override
	protected AbstractSTObject container() {
		return (AbstractSTObject) container;
	}

}
