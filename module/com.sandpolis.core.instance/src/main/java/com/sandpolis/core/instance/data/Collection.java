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
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

import com.sandpolis.core.foundation.util.RandUtil;
import com.sandpolis.core.instance.Attribute.ProtoCollection;

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

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, Document> documents;

	public Collection(Document parent) {
		this.db_id = UUID.randomUUID().toString();
		this.documents = new HashMap<>();
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

	@Override
	public void merge(ProtoCollection delta) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public ProtoCollection serialize() {
		// TODO Auto-generated method stub
		return null;
	}

}
