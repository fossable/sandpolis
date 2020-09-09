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
import java.util.function.Function;
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.state.AbstractSTObject;
import com.sandpolis.core.instance.state.EphemeralRelation;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.STRelation;
import com.sandpolis.core.instance.state.VirtObject;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.instance.store.StoreMetadata;
import com.sandpolis.core.server.hibernate.HibernateCollectionMetadata;

@Entity
public class HibernateCollection extends AbstractSTObject implements STCollection {

	@Id
	private String db_id;

	@Column(nullable = true)
	@Convert(converter = OidConverter.class)
	protected Oid oid;

	@Column(nullable = true)
	private HibernateDocument parent;

	@MapKeyColumn
	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, HibernateDocument> documents;

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

	public STDocument get(int key) {
		return documents.get(key);
	}

	@Override
	public Stream<STDocument> documents() {
		return documents.values().stream().map(STDocument.class::cast);
	}

	@Override
	public <E extends VirtObject> STRelation<E> collectionList(Function<STDocument, E> constructor) {
		return new EphemeralRelation<>(constructor);
	}

	@Override
	public int size() {
		return documents.size();
	}

	public boolean isEmpty() {
		return documents.isEmpty();
	}

	public boolean contains(STDocument document) {
		return documents.containsValue(document);
	}

	public void add(int tag, HibernateDocument e) {
		em.getTransaction().begin();
		documents.put(tag, e);
		em.flush();
		em.getTransaction().commit();
	}

	@Override
	public void remove(STDocument document) {
		documents.values().remove(document);

	}

	public void clear() {
		em.getTransaction().begin();
		documents.clear();
		em.flush();
		em.getTransaction().commit();
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

	@Override
	public HibernateDocument getDocument(int tag) {
		return documents.get(tag);
	}

	@Override
	public void setDocument(int tag, STDocument document) {
		documents.put(tag, (HibernateDocument) document);
		document.setOid(oid.child(tag));
	}

	@Override
	public void merge(ProtoCollection snapshot) {
		for (var entry : snapshot.getDocumentMap().entrySet()) {
			document(entry.getKey()).merge(entry.getValue());
		}

		if (!snapshot.getPartial()) {
			// Remove anything that wasn't in the snapshot
			documents.entrySet().removeIf(entry -> !snapshot.containsDocument(entry.getKey()));
		}
	}

	@Override
	public ProtoCollection snapshot(RelativeOid<?>... oids) {
		if (oids.length == 0) {
			var snapshot = ProtoCollection.newBuilder().setPartial(false);
			documents.forEach((tag, document) -> {
				snapshot.putDocument(tag, document.snapshot());
			});
			return snapshot.build();
		} else {
			var snapshot = ProtoCollection.newBuilder().setPartial(true);
			for (var head : Arrays.stream(oids).mapToInt(Oid::first).distinct().toArray()) {
				var children = Arrays.stream(oids).filter(oid -> oid.first() != head).map(Oid::tail)
						.toArray(RelativeOid[]::new);

				snapshot.putDocument(head, documents.get(head).snapshot(children));
			}

			return snapshot.build();
		}
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

	@Override
	public Oid oid() {
		return oid;
	}

	@Override
	public void setOid(Oid oid) {
		this.oid = oid;
	}
}
