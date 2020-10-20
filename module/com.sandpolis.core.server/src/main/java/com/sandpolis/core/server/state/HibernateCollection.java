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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.state.st.AbstractSTCollection;
import com.sandpolis.core.instance.state.st.STCollection;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.store.StoreMetadata;
import com.sandpolis.core.server.hibernate.HibernateCollectionMetadata;

@Entity
public class HibernateCollection extends AbstractSTCollection implements STCollection {

	@Id
	private String db_id;

	@Column
	public long getTag() {
		return tag;
	}

	@Column(nullable = true)
	protected HibernateDocument getParent() {
		return (HibernateDocument) parent;
	}

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	protected Map<Integer, HibernateDocument> getDocuments() {
		return (Map) documents;
	}

	@OneToOne(cascade = CascadeType.ALL)
	private HibernateCollectionMetadata metadata;

	@Transient
	EntityManager em;

	public HibernateCollection(HibernateDocument parent) {
		this.parent = parent;
		this.db_id = UUID.randomUUID().toString();

		documents = new HashMap<>();
	}

	public HibernateCollection(HibernateDocument parent, ProtoCollection collection) {
		this(parent);
		merge(collection);
	}

	protected HibernateCollection() {
		// JPA CONSTRUCTOR
	}

	@Override
	public void remove(STDocument document) {
		em.getTransaction().begin();
		documents.values().remove(document);
		em.flush();
		em.getTransaction().commit();
	}

	public void clear() {
		em.getTransaction().begin();
		documents.clear();
		em.flush();
		em.getTransaction().commit();
	}

	@Override
	public void setDocument(long tag, STDocument document) {
		em.getTransaction().begin();
		documents.put(tag, (HibernateDocument) document);
		document.setTag(tag);
		em.flush();
		em.getTransaction().commit();
	}

	@Override
	public STDocument newDocument() {
		return new HibernateDocument(this);
	}

	public STDocument newDetached(Function<STCollection, STDocument> cons) {
		return cons.apply(this);
	}

	@Override
	public StoreMetadata getMetadata() {
		return metadata;
	}

}
