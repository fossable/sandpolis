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
package com.sandpolis.core.profile.attribute;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

import com.sandpolis.core.instance.Attribute.ProtoDocument;
import com.sandpolis.core.instance.ProtoType;
import com.sandpolis.core.instance.Result.ErrorCode;
import com.sandpolis.core.profile.attribute.key.AttributeKey;

/**
 * An attribute document is primarily a set of {@link Attribute}s, but can also
 * have sub-documents or sub-collections. They can represent anything from a
 * particular running process or a particular disk.
 *
 * @author cilki
 * @since 5.1.1
 */
@Entity
public class Document implements ProtoType<ProtoDocument> {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int db_id;

	@Column
	private String id;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@MapKeyColumn
	private Map<String, Document> documents;

	public Document document(String id) {
		var document = documents.get(id);
		if (document == null) {
			document = new Document(id);
			documents.put(id, document);
		}
		return document;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@MapKeyColumn
	private Map<String, Collection> collections;

	public Collection collection(String id) {
		var collection = collections.get(id);
		if (collection == null) {
			collection = new Collection(id);
			collections.put(id, collection);
		}
		return collection;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@MapKeyColumn
	private Map<String, Attribute<?>> attributes;

	public <E> Attribute<E> attribute(AttributeKey<E> key, String id) {
		var attribute = attributes.get(id);
		if (attribute == null) {
			attribute = key.newAttribute();
			attributes.put(id, attribute);
		}
		return (Attribute<E>) attribute;
	}

	public Document(String id) {
		this.id = id;
		documents = new HashMap<>();
		collections = new HashMap<>();
		attributes = new HashMap<>();
	}

	protected Document() {
	}

	@Override
	public ErrorCode merge(ProtoDocument delta) throws Exception {
		// TODO Auto-generated method stub
		return ErrorCode.OK;
	}

	@Override
	public ProtoDocument extract() {
		// TODO Auto-generated method stub
		return null;
	}

}
