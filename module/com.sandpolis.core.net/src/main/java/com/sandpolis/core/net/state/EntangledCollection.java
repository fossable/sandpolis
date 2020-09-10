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
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.STRelation;
import com.sandpolis.core.instance.state.VirtObject;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.instance.store.StoreMetadata;
import com.sandpolis.core.net.state.STCmd.STSyncStruct;
import com.sandpolis.core.net.stream.StreamSink;
import com.sandpolis.core.net.stream.StreamSource;

public class EntangledCollection extends EntangledObject<ProtoCollection> implements STCollection {

	private STCollection container;

	public EntangledCollection(STCollection container, STSyncStruct config) {
		this.container = Objects.requireNonNull(container);

		if ((config.initiator && config.direction == DOWNSTREAM) || (!config.initiator && config.direction == UPSTREAM)
				|| config.direction == BIDIRECTIONAL) {
			sink = new StreamSink<>() {

				@Override
				public void onNext(ProtoCollection item) {
					container.merge(item);
				};
			};
		}

		if ((config.initiator && config.direction == UPSTREAM) || (!config.initiator && config.direction == DOWNSTREAM)
				|| config.direction == BIDIRECTIONAL) {
			source = new StreamSource<>() {

				@Override
				public void start() {
					container.addListener(EntangledCollection.this);
				}

				@Override
				public void stop() {
					container.removeListener(EntangledCollection.this);
				}
			};
			source.start();
		}
	}

	@Subscribe
	<T> void handle(STAttribute.ChangeEvent<T> event) {
		source.submit(test(event.attribute, event.newValue));
	}

	// Begin boilerplate

	@Override
	public void merge(ProtoCollection snapshot) {
		container.merge(snapshot);
	}

	@Override
	public ProtoCollection snapshot(RelativeOid<?>... oids) {
		return container.snapshot(oids);
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
	public int size() {
		return container.size();
	}

	@Override
	public <E extends VirtObject> STRelation<E> collectionList(Function<STDocument, E> constructor) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STDocument newDocument() {
		return container.newDocument();
	}

	@Override
	public StoreMetadata getMetadata() {
		return container.getMetadata();
	}

	@Override
	public void remove(STDocument document) {
		container.remove(document);
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

}
