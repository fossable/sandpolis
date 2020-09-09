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
package com.sandpolis.core.instance.state;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.instance.state.oid.AbsoluteOid.AbsoluteOidImpl;
import com.sandpolis.core.instance.store.StoreMetadata;

public class EphemeralCollection extends EphemeralObject implements STCollection {

	private EphemeralDocument parent;

	private Map<Integer, STDocument> documents;

	private final Metadata metadata = new Metadata();

	public EphemeralCollection(EphemeralDocument parent) {
		this.parent = parent;

		documents = new HashMap<>();
	}

	public EphemeralCollection(EphemeralDocument parent, ProtoCollection collection) {
		this(parent);
		merge(collection);
	}

	public STDocument get(int key) {
		return documents.get(key);
	}

	@Override
	public Stream<STDocument> documents() {
		return documents.values().stream();
	}

	@Override
	public <E extends VirtObject> STRelation<E> collectionList(Function<STDocument, E> constructor) {
		return new EphemeralRelation<>(constructor);
	}

	@Override
	public int size() {
		return documents.size();
	}

	public boolean isEmpty() {
		return documents.isEmpty();
	}

	public boolean contains(STDocument document) {
		return documents.containsValue(document);
	}

	public void add(int tag, EphemeralDocument e) {
		documents.put(tag, e);
		fireCollectionEvent(e, null);
	}

	@Override
	public void remove(STDocument document) {
		documents.values().remove(document);
		fireCollectionEvent(null, document);
	}

	public void clear() {
		documents.clear();
	}

	@Override
	public STDocument document(int tag) {
		var document = getDocument(tag);
		if (document == null) {
			document = new EphemeralDocument(this);
			setDocument(tag, document);
		}
		return document;
	}

	@Override
	public STDocument getDocument(int tag) {
		return documents.get(tag);
	}

	@Override
	public void setDocument(int tag, STDocument document) {
		documents.put(tag, document);
		document.setOid(oid == null ? new AbsoluteOidImpl<>(tag) : oid.child(tag));
	}

	@Override
	public STDocument newDocument() {
		return new EphemeralDocument(this);
	}

	@Override
	public void merge(ProtoCollection snapshot) {
		for (var entry : snapshot.getDocumentMap().entrySet()) {
			document(entry.getKey()).merge(entry.getValue());
		}

		if (!snapshot.getPartial()) {
			// Remove anything that wasn't in the snapshot
			documents.entrySet().removeIf(entry -> !snapshot.containsDocument(entry.getKey()));
		}
	}

	@Override
	public ProtoCollection snapshot(RelativeOid<?>... oids) {
		if (oids.length == 0) {
			var snapshot = ProtoCollection.newBuilder().setPartial(false);
			documents.forEach((tag, document) -> {
				snapshot.putDocument(tag, document.snapshot());
			});
			return snapshot.build();
		} else {
			var snapshot = ProtoCollection.newBuilder().setPartial(true);
			for (var head : Arrays.stream(oids).mapToInt(Oid::first).distinct().toArray()) {
				var children = Arrays.stream(oids).filter(oid -> oid.first() != head).map(Oid::tail)
						.toArray(RelativeOid[]::new);

				snapshot.putDocument(head, documents.get(head).snapshot(children));
			}

			return snapshot.build();
		}
	}

	@Override
	public StoreMetadata getMetadata() {
		return metadata;
	}

	private class Metadata implements StoreMetadata {

		@Override
		public int getInitCount() {
			return 1;
		}
	}
}
