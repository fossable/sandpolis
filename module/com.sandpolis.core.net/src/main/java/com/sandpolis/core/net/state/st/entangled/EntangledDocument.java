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
package com.sandpolis.core.net.state.st.entangled;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.oid.AbsoluteOid;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.instance.state.st.AbstractSTObject;
import com.sandpolis.core.instance.state.st.STAttribute;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.state.st.STObject;
import com.sandpolis.core.net.state.STCmd.STSyncStruct;

public class EntangledDocument extends EntangledObject<ProtoDocument> implements STDocument {

	private STDocument container;

	public EntangledDocument(STDocument container, STSyncStruct config) {
		super(container.parent(), container.oid());
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
	public Collection<STAttribute<?>> attributes() {
		return container.attributes();
	}

	@Override
	public Collection<STDocument> documents() {
		return container.documents();
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
	public STDocument parent() {
		return container.parent();
	}

	@Override
	public void removeListener(Object listener) {
		container.removeListener(listener);
	}

	@Override
	public ProtoDocument snapshot(RelativeOid... oids) {
		return container.snapshot(oids);
	}

	@Override
	protected STObject<ProtoDocument> container() {
		return container;
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
	public void remove(STDocument document) {
		// TODO Auto-generated method stub

	}

	@Override
	public int documentCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int attributeCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void forEachDocument(Consumer<STDocument> consumer) {
		// TODO Auto-generated method stub

	}

	@Override
	public <E> STAttribute<E> attribute(RelativeOid<STAttribute<E>> oid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STDocument document(RelativeOid<STDocument> oid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <E> STAttribute<E> getAttribute(RelativeOid<STAttribute<E>> oid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STDocument getDocument(RelativeOid<STDocument> oid) {
		// TODO Auto-generated method stub
		return null;
	}

}
