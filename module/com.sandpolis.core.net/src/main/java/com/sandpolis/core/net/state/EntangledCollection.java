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
import java.util.function.Function;
import java.util.stream.Stream;

import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.state.AbstractSTObject;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.STRelation;
import com.sandpolis.core.instance.state.VirtObject;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.instance.store.StoreMetadata;
import com.sandpolis.core.net.state.STCmd.STSyncStruct;

public class EntangledCollection extends EntangledObject<ProtoCollection> implements STCollection {

	private STCollection container;

	public EntangledCollection(STCollection container, STSyncStruct config) {
		this.container = Objects.requireNonNull(container);

		// Start streams
		switch (config.direction) {
		case BIDIRECTIONAL:
			startSource(config);
			startSink(config, ProtoCollection.class);
			break;
		case DOWNSTREAM:
			if (config.initiator) {
				startSink(config, ProtoCollection.class);
			} else {
				startSource(config);
			}
			break;
		case UPSTREAM:
			if (config.initiator) {
				startSource(config);
			} else {
				startSink(config, ProtoCollection.class);
			}
			break;
		default:
			throw new IllegalArgumentException();
		}
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
		return container.collectionList(constructor);
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
