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
package com.sandpolis.core.instance.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

import com.sandpolis.core.instance.Attribute.ProtoDocument;

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

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, Document> documents;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, Collection> collections;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, Attribute<?>> attributes;

	public Document(Document parent) {
		db_id = UUID.randomUUID().toString();
		documents = new HashMap<>();
		collections = new HashMap<>();
		attributes = new HashMap<>();
	}

	public Document(Collection parent) {
		db_id = UUID.randomUUID().toString();
		documents = new HashMap<>();
		collections = new HashMap<>();
		attributes = new HashMap<>();
	}

	protected Document() {
		// JPA CONSTRUCTOR
	}

	@SuppressWarnings("unchecked")
	public <E> Attribute<E> attribute(int tag) {
		Attribute<?> attribute = attributes.get(tag);
		if (attribute == null) {
			attribute = null;// TODO
			attributes.put(tag, attribute);
		}
		return (Attribute<E>) attribute;
	}

	@SuppressWarnings("unchecked")
	public <E> Attribute<E> attribute(int tag, Supplier<Attribute<E>> factory) {
		Attribute<?> attribute = attributes.get(tag);
		if (attribute == null) {
			attribute = factory.get();
			attributes.put(tag, attribute);
		}
		return (Attribute<E>) attribute;
	}

	public Document document(int tag) {
		Document document = documents.get(tag);
		if (document == null) {
			document = new Document(this);
			documents.put(tag, document);
		}
		return document;
	}

	public Collection collection(int tag) {
		Collection collection = collections.get(tag);
		if (collection == null) {
			collection = new Collection(this);
			collections.put(tag, collection);
		}
		return collection;
	}

	public String getId() {
		return db_id;
	}

	@Override
	public void merge(ProtoDocument delta) throws Exception {
		for (var entry : delta.getDocumentMap().entrySet()) {
			document(entry.getKey()).merge(entry.getValue());
		}
		for (var entry : delta.getCollectionMap().entrySet()) {
			collection(entry.getKey()).merge(entry.getValue());
		}
		for (var entry : delta.getAttributeMap().entrySet()) {
			attribute(entry.getKey()).merge(entry.getValue());
		}
	}

	@Override
	public ProtoDocument serialize() {
		var serial = ProtoDocument.newBuilder();
		documents.forEach((tag, document) -> {
			serial.putDocument(tag, document.serialize());
		});
		collections.forEach((tag, collection) -> {
			serial.putCollection(tag, collection.serialize());
		});
		attributes.forEach((tag, attribute) -> {
			serial.putAttribute(tag, attribute.serialize());
		});
		return serial.build();
	}
}
