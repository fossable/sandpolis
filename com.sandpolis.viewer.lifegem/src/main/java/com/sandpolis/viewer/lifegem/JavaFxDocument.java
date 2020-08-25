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
package com.sandpolis.viewer.lifegem;

import java.util.stream.Stream;

import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.DefaultDocument;
import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.VirtObject;

public class JavaFxDocument<T extends VirtObject> implements STDocument {

	private DefaultDocument container;

	@Override
	public void merge(ProtoDocument snapshot) {
		container.merge(snapshot);
	}

	@Override
	public ProtoDocument snapshot(Oid<?>... oids) {
		return container.snapshot(oids);
	}

	@Override
	public <E> STAttribute<E> attribute(int tag) {
		return container.attribute(tag);
	}

	@Override
	public STCollection collection(int tag) {
		return container.collection(tag);
	}

	@Override
	public STDocument document(int tag) {
		return container.document(tag);
	}

	@Override
	public <E> STAttribute<E> getAttribute(int tag) {
		return container.getAttribute(tag);
	}

	@Override
	public STCollection getCollection(int tag) {
		return container.getCollection(tag);
	}

	@Override
	public STDocument getDocument(int tag) {
		return container.getDocument(tag);
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Oid<?> getOid() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAttribute(int tag, STAttribute<?> attribute) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCollection(int tag, STCollection collection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDocument(int tag, STDocument document) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setOid(Oid<?> oid) {
		// TODO Auto-generated method stub

	}

	@Override
	public Stream<STAttribute<?>> attributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stream<STCollection> collections() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stream<STCollection> documents() {
		// TODO Auto-generated method stub
		return null;
	}

}