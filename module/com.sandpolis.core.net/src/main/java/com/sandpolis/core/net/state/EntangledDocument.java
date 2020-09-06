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
import java.util.stream.Stream;

import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.oid.AbsoluteOid;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.net.state.STCmd.STSyncStruct;

public class EntangledDocument implements STDocument {

	private STDocument container;

	public EntangledDocument(STDocument container, STSyncStruct config) {
		this.container = Objects.requireNonNull(container);
	}

	// Begin boilerplate

	@Override
	public void merge(ProtoDocument snapshot) {
		// TODO Auto-generated method stub

	}

	@Override
	public ProtoDocument snapshot(RelativeOid<?>... oids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <E> STAttribute<E> attribute(int tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stream<STAttribute<?>> attributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <E> STAttribute<E> getAttribute(int tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAttribute(int tag, STAttribute<?> attribute) {
		// TODO Auto-generated method stub

	}

	@Override
	public STCollection collection(int tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stream<STCollection> collections() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STCollection getCollection(int tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCollection(int tag, STCollection collection) {
		// TODO Auto-generated method stub

	}

	@Override
	public STDocument document(int tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stream<STDocument> documents() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STDocument getDocument(int tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDocument(int tag, STDocument document) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AbsoluteOid<?> getOid() {
		// TODO Auto-generated method stub
		return null;
	}

}
