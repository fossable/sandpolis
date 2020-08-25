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

import com.sandpolis.core.instance.State.ProtoDocument;

/**
 * {@link DefaultDocument} allows documents to be persistent.
 *
 * @since 5.1.1
 */
@Entity
public class DefaultDocument implements STDocument {

	@Id
	private String db_id;

	@Column
	@Convert(converter = OidConverter.class)
	private Oid<?> oid;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, STDocument> documents;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, STCollection> collections;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, STAttribute<?>> attributes;

	public DefaultDocument(Oid<?> oid) {
		this.db_id = UUID.randomUUID().toString();
		this.oid = oid;

		documents = new HashMap<>();
		collections = new HashMap<>();
		attributes = new HashMap<>();
	}

	public DefaultDocument(Oid<?> oid, ProtoDocument document) {
		this(oid);
		merge(document);
	}

	protected DefaultDocument() {
		// JPA CONSTRUCTOR
	}

	@Override
	public Oid<?> getOid() {
		return oid;
	}

	public void setOid(Oid<?> oid) {
		this.oid = oid;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> DefaultAttribute<E> attribute(int tag) {
		var attribute = attributes.get(tag);
		if (attribute == null) {
			attribute = new DefaultAttribute<>(oid.child(tag));
			attributes.put(tag, attribute);
		}
		return (DefaultAttribute<E>) attribute;
	}

	@Override
	public <E> STAttribute<E> getAttribute(int tag) {
		return (STAttribute<E>) attributes.get(tag);
	}

	@Override
	public void setAttribute(int tag, STAttribute<?> attribute) {
		attributes.put(tag, (DefaultAttribute<?>) attribute);
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

	public STDocument getDocument(int tag) {
		return documents.get(tag);
	}

	@Override
	public void setDocument(int tag, STDocument document) {
		documents.put(tag, (DefaultDocument) document);
	}

	@Override
	public STCollection collection(int tag) {
		var collection = collections.get(tag);
		if (collection == null) {
			collection = new DefaultCollection(oid.child(tag));
			collections.put(tag, collection);
		}
		return collection;
	}

	@Override
	public STCollection getCollection(int tag) {
		return collections.get(tag);
	}

	@Override
	public void setCollection(int tag, STCollection collection) {
		collections.put(tag, (DefaultCollection) collection);
	}

	@Override
	public String getId() {
		return db_id;
	}

	@Override
	public void merge(ProtoDocument snapshot) {
		for (var entry : snapshot.getDocumentMap().entrySet()) {
			document(entry.getKey()).merge(entry.getValue());
		}
		for (var entry : snapshot.getCollectionMap().entrySet()) {
			collection(entry.getKey()).merge(entry.getValue());
		}
		for (var entry : snapshot.getAttributeMap().entrySet()) {
			attribute(entry.getKey()).merge(entry.getValue());
		}

		if (!snapshot.getPartial()) {
			// Remove anything that wasn't in the snapshot
			documents.entrySet().removeIf(entry -> !snapshot.containsDocument(entry.getKey()));
			collections.entrySet().removeIf(entry -> !snapshot.containsCollection(entry.getKey()));
			attributes.entrySet().removeIf(entry -> !snapshot.containsAttribute(entry.getKey()));
		}
	}

	@Override
	public ProtoDocument snapshot(Oid<?>... oids) {
		if (oids.length == 0) {
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
		} else {
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

	@Override
	public Stream<STAttribute<?>> attributes() {
		return attributes.values().stream();
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
