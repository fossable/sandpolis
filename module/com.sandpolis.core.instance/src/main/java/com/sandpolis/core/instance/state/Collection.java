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
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

import com.sandpolis.core.instance.State.ProtoCollection;

/**
 * A collection is an unordered set of {@link Document}s. Every document has an
 * associated non-zero "tag" which is a function of the document's identity.
 *
 * @author cilki
 * @since 5.1.1
 */
@Entity
public class Collection implements ProtoType<ProtoCollection> {

	@Id
	private String db_id;

	@Column
	@Convert(converter = OidConverter.class)
	private Oid<?> oid;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, Document> documents;

	public Collection(Oid<?> oid) {
		this.db_id = UUID.randomUUID().toString();
		this.documents = new HashMap<>();
	}

	public Collection(Oid<?> oid, ProtoCollection collection) {
		this(oid);
		merge(collection);
	}

	protected Collection() {
		// JPA CONSTRUCTOR
	}

	public Document get(int key) {
		return documents.get(key);
	}

	public Stream<Document> stream() {
		return documents.values().stream();
	}

	public int size() {
		return documents.size();
	}

	public boolean isEmpty() {
		return documents.isEmpty();
	}

	public boolean contains(Document document) {
		return documents.containsValue(document);
	}

	public void add(int tag, Document e) {
		documents.put(tag, e);
	}

	public boolean remove(Document document) {
		return documents.values().remove(document);
	}

	public void clear() {
		documents.clear();
	}

	public Document document(int tag) {
		Document document = documents.get(tag);
		if (document == null) {
			document = new Document(oid.child(tag));
			documents.put(tag, document);
		}
		return document;
	}

	@Override
	public void merge(ProtoCollection delta) {
		for (var entry : delta.getDocumentMap().entrySet()) {
			document(entry.getKey()).merge(entry.getValue());
		}

		if (!delta.getPartial()) {
			// Remove anything that wasn't in the snapshot
			documents.entrySet().removeIf(entry -> !delta.containsDocument(entry.getKey()));
		}
	}

	@Override
	public ProtoCollection snapshot() {
		var snapshot = ProtoCollection.newBuilder().setPartial(false);
		documents.forEach((tag, document) -> {
			snapshot.putDocument(tag, document.snapshot());
		});
		return snapshot.build();
	}

	@Override
	public ProtoCollection snapshot(Oid<?>... oids) {
		var snapshot = ProtoCollection.newBuilder().setPartial(true);
		for (var head : Arrays.stream(oids).mapToInt(Oid::head).distinct().toArray()) {
			var children = Arrays.stream(oids).filter(oid -> oid.head() != head).map(Oid::tail).toArray(Oid[]::new);

			snapshot.putDocument(head, documents.get(head).snapshot(children));
		}

		return snapshot.build();
	}

}
