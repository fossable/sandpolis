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
package com.sandpolis.core.server.state;

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
import com.sandpolis.core.instance.state.AbstractSTObject;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;

/**
 * {@link HibernateDocument} allows documents to be persistent.
 *
 * @since 5.1.1
 */
@Entity
public class HibernateDocument extends AbstractSTObject implements STDocument {

	@Id
	private String db_id;

	@Column(nullable = true)
	@Convert(converter = OidConverter.class)
	protected Oid oid;

	@Column(nullable = true)
	private HibernateDocument parentDocument;

	@Column(nullable = true)
	private HibernateCollection parentCollection;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, HibernateDocument> documents;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, HibernateCollection> collections;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, HibernateAttribute<?>> attributes;

	public HibernateDocument(HibernateDocument parent) {
		this.parentDocument = parent;
		this.db_id = UUID.randomUUID().toString();

		documents = new HashMap<>();
		collections = new HashMap<>();
		attributes = new HashMap<>();
	}

	public HibernateDocument(HibernateDocument parent, ProtoDocument document) {
		this(parent);
		merge(document);
	}

	public HibernateDocument(HibernateCollection parent) {
		this.parentCollection = parent;
		this.db_id = UUID.randomUUID().toString();

		documents = new HashMap<>();
		collections = new HashMap<>();
		attributes = new HashMap<>();
	}

	public HibernateDocument(HibernateCollection parent, ProtoDocument document) {
		this(parent);
		merge(document);
	}

	protected HibernateDocument() {
		// JPA CONSTRUCTOR
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> HibernateAttribute<E> attribute(int tag) {
		var attribute = getAttribute(tag);
		if (attribute == null) {
			attribute = new HibernateAttribute<>(this);
			setAttribute(tag, attribute);
		}
		return (HibernateAttribute<E>) attribute;
	}

	@Override
	public <E> HibernateAttribute<E> getAttribute(int tag) {
		return (HibernateAttribute<E>) attributes.get(tag);
	}

	@Override
	public void setAttribute(int tag, STAttribute<?> attribute) {
		attributes.put(tag, (HibernateAttribute<?>) attribute);
		attribute.setOid(oid.child(tag));
	}

	@Override
	public HibernateDocument document(int tag) {
		var document = getDocument(tag);
		if (document == null) {
			document = new HibernateDocument(this);
			setDocument(tag, document);
		}
		return document;
	}

	public HibernateDocument getDocument(int tag) {
		return documents.get(tag);
	}

	@Override
	public void setDocument(int tag, STDocument document) {
		documents.put(tag, (HibernateDocument) document);
		document.setOid(oid.child(tag));
	}

	@Override
	public HibernateCollection collection(int tag) {
		var collection = getCollection(tag);
		if (collection == null) {
			collection = new HibernateCollection(this);
			setCollection(tag, collection);
		}
		return collection;
	}

	@Override
	public HibernateCollection getCollection(int tag) {
		return collections.get(tag);
	}

	@Override
	public void setCollection(int tag, STCollection collection) {
		collections.put(tag, (HibernateCollection) collection);
		collection.setOid(oid.child(tag));
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
	public ProtoDocument snapshot(RelativeOid<?>... oids) {
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
			for (var head : Arrays.stream(oids).mapToInt(Oid::first).distinct().toArray()) {
				var children = Arrays.stream(oids).filter(oid -> oid.first() != head).map(Oid::tail)
						.toArray(RelativeOid[]::new);

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
		return attributes.values().stream().map(STAttribute.class::cast);
	}

	@Override
	public Stream<STCollection> collections() {
		return collections.values().stream().map(STCollection.class::cast);
	}

	@Override
	public Stream<STDocument> documents() {
		return documents.values().stream().map(STDocument.class::cast);
	}

	@Override
	public Oid oid() {
		return oid;
	}

	@Override
	public void setOid(Oid oid) {
		this.oid = oid;
	}
}
