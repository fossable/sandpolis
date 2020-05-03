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
import java.util.Iterator;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

/**
 * A collection is an unordered set of {@link Document}s. Every document has an
 * associated non-zero "tag" which is a function of the document's identity.
 *
 * @author cilki
 * @since 5.1.1
 */
@Entity
public class Collection implements java.util.Collection<Document> {

	@Id
	@GeneratedValue(generator = "uuid")
//	@GenericGenerator(name = "uuid", strategy = "uuid2")
	private String db_id;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, Document> documents;

	public Collection(Document parent) {
		this.documents = new HashMap<>();
	}

	protected Collection() {
		// JPA CONSTRUCTOR
	}

	@Override
	public int size() {
		return documents.size();
	}

	@Override
	public boolean isEmpty() {
		return documents.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return documents.containsValue(o);
	}

	@Override
	public Iterator<Document> iterator() {
		return documents.values().iterator();
	}

	@Override
	public Object[] toArray() {
		return documents.values().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return documents.values().toArray(a);
	}

	@Override
	public boolean add(Document e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean remove(Object o) {
		return documents.values().remove(o);
	}

	@Override
	public boolean containsAll(java.util.Collection<?> c) {
		return documents.values().containsAll(c);
	}

	@Override
	public boolean addAll(java.util.Collection<? extends Document> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAll(java.util.Collection<?> c) {
		return documents.values().removeAll(c);
	}

	@Override
	public boolean retainAll(java.util.Collection<?> c) {
		return documents.values().retainAll(c);
	}

	@Override
	public void clear() {
		documents.clear();
	}

}
