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

import static com.sandpolis.core.net.msg.MsgState.RQ_STSync.STSyncDirection.BIDIRECTIONAL;
import static com.sandpolis.core.net.msg.MsgState.RQ_STSync.STSyncDirection.DOWNSTREAM;
import static com.sandpolis.core.net.msg.MsgState.RQ_STSync.STSyncDirection.UPSTREAM;

import java.util.Objects;
import java.util.function.Supplier;

import com.sandpolis.core.instance.State.ProtoAttribute;
import com.sandpolis.core.instance.state.AbstractSTObject;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.net.state.STCmd.STSyncStruct;
import com.sandpolis.core.net.stream.StreamSink;
import com.sandpolis.core.net.stream.StreamSource;

public class EntangledAttribute<T> extends EntangledObject<ProtoAttribute> implements STAttribute<T> {

	private STAttribute<T> container;

	public EntangledAttribute(STAttribute<T> container, STSyncStruct config) {
		this.container = Objects.requireNonNull(container);

		if ((config.initiator && config.direction == DOWNSTREAM) || (!config.initiator && config.direction == UPSTREAM)
				|| config.direction == BIDIRECTIONAL) {
			sink = new StreamSink<>() {

				@Override
				public void onNext(ProtoAttribute item) {
					container.merge(item);
				};
			};
		}

		if ((config.initiator && config.direction == UPSTREAM) || (!config.initiator && config.direction == DOWNSTREAM)
				|| config.direction == BIDIRECTIONAL) {
			source = new StreamSource<>() {

				@Override
				public void start() {
					container.addListener(EntangledAttribute.this);
				}

				@Override
				public void stop() {
					container.removeListener(EntangledAttribute.this);
				}
			};
			source.start();
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
	public void setOid(Oid oid) {
		container.setOid(oid);
	}

	@Override
	public AbstractSTObject parent() {
		return ((AbstractSTObject) container).parent();
	}

}
