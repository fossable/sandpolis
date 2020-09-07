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

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.STRelation;
import com.sandpolis.core.instance.state.VirtObject;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.instance.store.StoreMetadata;

import javafx.collections.ObservableListBase;

public class FxCollection<T extends VirtObject> extends ObservableListBase<T> implements STCollection {

	private STCollection container;

	public FxCollection(STCollection container, Function<STDocument, T> constructor) {
		this.container = Objects.requireNonNull(container);
	}

	@Override
	public T get(int index) {
		return null;
	}

	@Override
	public int size() {
		return container.size();
	}

	@Override
	public T set(int index, T element) {
		// TODO Auto-generated method stub
		return super.set(index, element);
	}

	@Override
	public void merge(ProtoCollection snapshot) {
		container.merge(snapshot);
	}

	@Override
	public ProtoCollection snapshot(RelativeOid<?>... oids) {
		return container.snapshot(oids);
	}

	@Override
	public <E extends VirtObject> STRelation<E> collectionList(Function<STDocument, E> constructor) {
		return container.collectionList(constructor);
	}

	@Override
	public STDocument document(int tag) {
		return container.document(tag);
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
	public Stream<T> stream() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stream<STDocument> documents() {
		return container.documents();
	}

	@Override
	public STDocument newDocument() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StoreMetadata getMetadata() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(STDocument document) {
		// TODO Auto-generated method stub

	}
}
