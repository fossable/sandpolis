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
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

import com.sandpolis.core.instance.State.ProtoCollection;

@Entity
public class DefaultCollection implements STCollection {

	@Id
	private String db_id;

	@Column
	@Convert(converter = OidConverter.class)
	private Oid<?> oid;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, STDocument> documents;

	public DefaultCollection(Oid<?> oid) {
		this.db_id = UUID.randomUUID().toString();
		this.oid = oid;

		documents = new HashMap<>();
	}

	public DefaultCollection(Oid<?> oid, ProtoCollection collection) {
		this(oid);
		merge(collection);
	}

	protected DefaultCollection() {
		// JPA CONSTRUCTOR
	}

	public Oid<?> getOid() {
		return oid;
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
		return new DefaultRelation<>(constructor);
	}

	public int size() {
		return documents.size();
	}

	public boolean isEmpty() {
		return documents.isEmpty();
	}

	public boolean contains(STDocument document) {
		return documents.containsValue(document);
	}

	public void add(int tag, DefaultDocument e) {
		documents.put(tag, e);
	}

	public boolean remove(STDocument document) {
		return documents.values().remove(document);
	}

	public void clear() {
		documents.clear();
	}

	@Override
	public STDocument document(int tag) {
		var document = documents.get(tag);
		if (document == null) {
			document = new DefaultDocument(oid.child(tag));
			documents.put(tag, document);
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
	public ProtoCollection snapshot(Oid<?>... oids) {
		if (oids.length == 0) {
			var snapshot = ProtoCollection.newBuilder().setPartial(false);
			documents.forEach((tag, document) -> {
				snapshot.putDocument(tag, document.snapshot());
			});
			return snapshot.build();
		} else {
			var snapshot = ProtoCollection.newBuilder().setPartial(true);
			for (var head : Arrays.stream(oids).mapToInt(Oid::head).distinct().toArray()) {
				var children = Arrays.stream(oids).filter(oid -> oid.head() != head).map(Oid::tail).toArray(Oid[]::new);

				snapshot.putDocument(head, documents.get(head).snapshot(children));
			}

			return snapshot.build();
		}
	}
}
