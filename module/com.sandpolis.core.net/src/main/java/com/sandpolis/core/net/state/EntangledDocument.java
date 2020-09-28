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

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.AbstractSTObject;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.STObject;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.net.state.STCmd.STSyncStruct;

public class EntangledDocument extends EntangledObject<ProtoDocument> implements STDocument {

	private STDocument container;

	public EntangledDocument(STDocument container, STSyncStruct config) {
		this.container = Objects.requireNonNull(container);

		if (container instanceof EntangledObject)
			throw new IllegalArgumentException("Entanged objects cannot be nested");

		// Start streams
		switch (config.direction) {
		case BIDIRECTIONAL:
			startSource(config);
			startSink(config, ProtoDocument.class);
			break;
		case DOWNSTREAM:
			if (config.initiator) {
				startSink(config, ProtoDocument.class);
			} else {
				startSource(config);
			}
			break;
		case UPSTREAM:
			if (config.initiator) {
				startSource(config);
			} else {
				startSink(config, ProtoDocument.class);
			}
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	// Begin boilerplate

	@Override
	public void addListener(Object listener) {
		container.addListener(listener);
	}

	@Override
	public <E> STAttribute<E> attribute(long tag) {
		return container.attribute(tag);
	}

	@Override
	public Collection<STAttribute<?>> attributes() {
		return container.attributes();
	}

	@Override
	public STCollection collection(long tag) {
		return container.collection(tag);
	}

	@Override
	public Collection<STCollection> collections() {
		return container.collections();
	}

	@Override
	public STDocument document(long tag) {
		return container.document(tag);
	}

	@Override
	public Collection<STDocument> documents() {
		return container.documents();
	}

	@Override
	public <E> STAttribute<E> getAttribute(long tag) {
		return container.getAttribute(tag);
	}

	@Override
	public STCollection getCollection(long tag) {
		return container.getCollection(tag);
	}

	@Override
	public STDocument getDocument(long tag) {
		return container.getDocument(tag);
	}

	@Override
	public String getId() {
		return container.getId();
	}

	@Override
	public long getTag() {
		return ((AbstractSTObject) container).getTag();
	}

	@Override
	public void merge(ProtoDocument snapshot) {
		container.merge(snapshot);
	}

	@Override
	public Oid oid() {
		return container.oid();
	}

	@Override
	public AbstractSTObject parent() {
		return ((AbstractSTObject) container).parent();
	}

	@Override
	public void removeListener(Object listener) {
		container.removeListener(listener);
	}

	@Override
	public void setAttribute(long tag, STAttribute<?> attribute) {
		container.setAttribute(tag, attribute);
	}

	@Override
	public void setCollection(long tag, STCollection collection) {
		container.setCollection(tag, collection);
	}

	@Override
	public void setDocument(long tag, STDocument document) {
		container.setDocument(tag, document);
	}

	@Override
	public void setTag(long tag) {
		container.setTag(tag);
	}

	@Override
	public ProtoDocument snapshot(RelativeOid<?>... oids) {
		return container.snapshot(oids);
	}

	@Override
	protected STObject<ProtoDocument> container() {
		return container;
	}

	@Override
	public STAttribute<?> newAttribute() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(STAttribute<?> attribute) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void forEachAttribute(Consumer<STAttribute<?>> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public STDocument newDocument() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(STDocument document) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void forEachDocument(Consumer<STDocument> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public STCollection newCollection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(STCollection collection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void forEachCollection(Consumer<STCollection> consumer) {
		// TODO Auto-generated method stub
		
	}
}
