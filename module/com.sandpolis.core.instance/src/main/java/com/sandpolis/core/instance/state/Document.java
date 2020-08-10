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
import java.util.function.Supplier;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

import com.sandpolis.core.instance.State.ProtoDocument;

/**
 * A document is a group of attributes associated with the entity that the
 * document represents.
 *
 * @author cilki
 * @since 5.1.1
 */
@Entity
public class Document implements ProtoType<ProtoDocument> {

	@Id
	private String db_id;

	@Column
	@Convert(converter = OidConverter.class)
	private Oid<?> oid;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, Document> documents;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, Collection> collections;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, Attribute<?>> attributes;

	public Document(Oid<?> oid) {
		db_id = UUID.randomUUID().toString();
		documents = new HashMap<>();
		collections = new HashMap<>();
		attributes = new HashMap<>();
	}

	public Document(Oid<?> oid, ProtoDocument document) {
		this(oid);
		merge(document);
	}

	protected Document() {
		// JPA CONSTRUCTOR
	}

	@SuppressWarnings("unchecked")
	public <E> Attribute<E> attribute(int tag) {
		Attribute<?> attribute = attributes.get(tag);
		if (attribute == null) {
			attribute = new Attribute<>(oid.child(tag));
			attributes.put(tag, attribute);
		}
		return (Attribute<E>) attribute;
	}

	public Document document(int tag) {
		Document document = documents.get(tag);
		if (document == null) {
			document = new Document(oid.child(tag));
			documents.put(tag, document);
		}
		return document;
	}

	public Collection collection(int tag) {
		Collection collection = collections.get(tag);
		if (collection == null) {
			collection = new Collection(oid.child(tag));
			collections.put(tag, collection);
		}
		return collection;
	}

	public String getId() {
		return db_id;
	}

	@Override
	public void merge(ProtoDocument delta) {
		for (var entry : delta.getDocumentMap().entrySet()) {
			document(entry.getKey()).merge(entry.getValue());
		}
		for (var entry : delta.getCollectionMap().entrySet()) {
			collection(entry.getKey()).merge(entry.getValue());
		}
		for (var entry : delta.getAttributeMap().entrySet()) {
			attribute(entry.getKey()).merge(entry.getValue());
		}

		if (!delta.getPartial()) {
			// Remove anything that wasn't in the snapshot
			documents.entrySet().removeIf(entry -> !delta.containsDocument(entry.getKey()));
			collections.entrySet().removeIf(entry -> !delta.containsCollection(entry.getKey()));
			attributes.entrySet().removeIf(entry -> !delta.containsAttribute(entry.getKey()));
		}
	}

	@Override
	public ProtoDocument snapshot() {
		var snapshot = ProtoDocument.newBuilder().setPartial(false);
		documents.forEach((tag, document) -> {
			snapshot.putDocument(tag, document.snapshot());
		});
		collections.forEach((tag, collection) -> {
			snapshot.putCollection(tag, collection.snapshot());
		});
		attributes.forEach((tag, attribute) -> {
			snapshot.putAttribute(tag, attribute.snapshot());
		});
		return snapshot.build();
	}

	@Override
	public ProtoDocument snapshot(Oid<?>... oids) {
		var snapshot = ProtoDocument.newBuilder().setPartial(true);
		for (var head : Arrays.stream(oids).mapToInt(Oid::head).distinct().toArray()) {
			var children = Arrays.stream(oids).filter(oid -> oid.head() != head).map(Oid::tail).toArray(Oid[]::new);

			if (documents.containsKey(head))
				snapshot.putDocument(head, documents.get(head).snapshot(children));
			if (collections.containsKey(head))
				snapshot.putCollection(head, collections.get(head).snapshot(children));
			if (attributes.containsKey(head))
				snapshot.putAttribute(head, attributes.get(head).snapshot());
		}

		return snapshot.build();
	}
}
