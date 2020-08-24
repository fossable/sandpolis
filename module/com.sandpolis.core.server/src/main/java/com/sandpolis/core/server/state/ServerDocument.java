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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.core.instance.state.OidConverter;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.STCollection;

/**
 * A document is a group of attributes associated with the entity that the
 * document represents.
 *
 * @author cilki
 * @since 5.1.1
 */
@Entity
public class ServerDocument implements STDocument {

	@Id
	private String db_id;

	@Column
	@Convert(converter = OidConverter.class)
	private Oid<?> oid;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, ServerDocument> documents;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, ServerCollection> collections;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, ServerAttribute<?>> attributes;

	public ServerDocument(Oid<?> oid) {
		this.oid = oid;
		db_id = UUID.randomUUID().toString();
		documents = new HashMap<>();
		collections = new HashMap<>();
		attributes = new HashMap<>();
	}

	public ServerDocument(Oid<?> oid, ProtoDocument document) {
		this(oid);
		merge(document);
	}

	protected ServerDocument() {
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
	public <E> ServerAttribute<E> attribute(int tag) {
		ServerAttribute<?> attribute = attributes.get(tag);
		if (attribute == null) {
			attribute = new ServerAttribute<>(oid.child(tag));
			attributes.put(tag, attribute);
		}
		return (ServerAttribute<E>) attribute;
	}

	@Override
	public ServerDocument document(int tag) {
		ServerDocument document = documents.get(tag);
		if (document == null) {
			document = new ServerDocument(oid.child(tag));
			documents.put(tag, document);
		}
		return document;
	}

	@Override
	public ServerCollection collection(int tag) {
		ServerCollection collection = collections.get(tag);
		if (collection == null) {
			collection = new ServerCollection(oid.child(tag));
			collections.put(tag, collection);
		}
		return collection;
	}

	public ServerDocument getDocument(int tag) {
		return documents.get(tag);
	}

	@Override
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
	public <E> STAttribute<E> getAttribute(int tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STCollection getCollection(int tag) {
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
}
