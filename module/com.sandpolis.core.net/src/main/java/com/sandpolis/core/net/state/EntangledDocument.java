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
import java.util.stream.Stream;

import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.AbstractSTObject;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.net.state.STCmd.STSyncStruct;
import com.sandpolis.core.net.stream.StreamSink;
import com.sandpolis.core.net.stream.StreamSource;

public class EntangledDocument extends EntangledObject<ProtoDocument> implements STDocument {

	private STDocument container;

	public EntangledDocument(STDocument container, STSyncStruct config) {
		this.container = Objects.requireNonNull(container);

		if ((config.initiator && config.direction == DOWNSTREAM) || (!config.initiator && config.direction == UPSTREAM)
				|| config.direction == BIDIRECTIONAL) {
			sink = new StreamSink<>() {

				@Override
				public void onNext(ProtoDocument item) {
					container.merge(item);
				};
			};
		}

		if ((config.initiator && config.direction == UPSTREAM) || (!config.initiator && config.direction == DOWNSTREAM)
				|| config.direction == BIDIRECTIONAL) {
			source = new StreamSource<>() {

				@Override
				public void start() {
					container.addListener(EntangledDocument.this);
				}

				@Override
				public void stop() {
					container.removeListener(EntangledDocument.this);
				}
			};
			source.start();
		}
	}

	// Begin boilerplate

	@Override
	public void merge(ProtoDocument snapshot) {
		container.merge(snapshot);
	}

	@Override
	public ProtoDocument snapshot(RelativeOid<?>... oids) {
		return container.snapshot(oids);
	}

	@Override
	public <E> STAttribute<E> attribute(int tag) {
		return container.attribute(tag);
	}

	@Override
	public Stream<STAttribute<?>> attributes() {
		return container.attributes();
	}

	@Override
	public <E> STAttribute<E> getAttribute(int tag) {
		return container.getAttribute(tag);
	}

	@Override
	public void setAttribute(int tag, STAttribute<?> attribute) {
		container.setAttribute(tag, attribute);
	}

	@Override
	public STCollection collection(int tag) {
		return container.collection(tag);
	}

	@Override
	public Stream<STCollection> collections() {
		return container.collections();
	}

	@Override
	public STCollection getCollection(int tag) {
		return container.getCollection(tag);
	}

	@Override
	public void setCollection(int tag, STCollection collection) {
		container.setCollection(tag, collection);
	}

	@Override
	public STDocument document(int tag) {
		return container.document(tag);
	}

	@Override
	public Stream<STDocument> documents() {
		return container.documents();
	}

	@Override
	public STDocument getDocument(int tag) {
		return container.getDocument(tag);
	}

	@Override
	public void setDocument(int tag, STDocument document) {
		container.setDocument(tag, document);
	}

	@Override
	public String getId() {
		return container.getId();
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
	public void addListener(Object listener) {
		container.addListener(listener);
	}

	@Override
	public void removeListener(Object listener) {
		container.removeListener(listener);
	}

	@Override
	public AbstractSTObject parent() {
		return ((AbstractSTObject) container).parent();
	}
}
