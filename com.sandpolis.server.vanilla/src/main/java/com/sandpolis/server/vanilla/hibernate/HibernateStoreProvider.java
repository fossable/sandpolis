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
package com.sandpolis.server.vanilla.hibernate;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import com.sandpolis.core.instance.data.Collection;
import com.sandpolis.core.instance.data.Document;
import com.sandpolis.core.instance.data.StateObject;
import com.sandpolis.core.instance.store.StoreMetadata;
import com.sandpolis.core.instance.store.provider.StoreProvider;

/**
 * A persistent {@link StoreProvider} that is backed by a Hibernate connection.
 *
 * @author cilki
 * @since 5.0.0
 */
@Entity
public class HibernateStoreProvider<E extends StateObject> implements StoreProvider<E> {

	@Id
	private String id;

	@OneToOne(cascade = CascadeType.ALL)
	private Collection collection;

	@OneToOne(cascade = CascadeType.ALL)
	private HibernateStoreProviderMetadata metadata;

	@Transient
	EntityManager em;

	@Transient
	Function<Document, E> constructor;

	@Transient
	Class<E> type;

	public HibernateStoreProvider(Class<E> type) {
		this.type = type;
		this.id = type.getName();
		this.collection = new Collection(null);
		this.metadata = new HibernateStoreProviderMetadata();
	}

	HibernateStoreProvider() {
		// JPA constructor
	}

	@Override
	public synchronized void add(E e) {
		em.getTransaction().begin();
		collection.add(e.tag(), e.getDocument());
		em.flush();
		em.getTransaction().commit();
	}

	@Override
	public synchronized Optional<E> get(Object id) {
		var document = collection.get((int) id);
		if (document == null)
			return Optional.empty();

		return Optional.of(constructor.apply(document));
	}

	@Override
	public synchronized long count() {
		return collection.size();
	}

	@Override
	public synchronized void remove(E e) {
		em.getTransaction().begin();
		collection.remove(e.getDocument());
		em.flush();
		em.getTransaction().commit();
	}

	@Override
	public synchronized void removeIf(Predicate<E> condition) {
		em.getTransaction().begin();
		stream().filter(condition).map(E::getDocument).forEach(collection::remove);
		em.flush();
		em.getTransaction().commit();
	}

	@Override
	public synchronized void clear() {
		em.getTransaction().begin();
		collection.clear();
		em.flush();
		em.getTransaction().commit();
	}

	@Override
	public Collection getCollection() {
		return collection;
	}

	@Override
	public synchronized java.util.Collection<E> enumerate() {
		return collection.stream().map(constructor).collect(Collectors.toList());
	}

	@Override
	public synchronized java.util.Collection<E> enumerate(String query, Object... params) {
		var q = em.createQuery(query, type);
		for (int i = 0; i < params.length; i++) {
			q.setParameter(i, params[i]);
		}

		return q.getResultList();
	}

	@Override
	public void initialize() {
		em.getTransaction().begin();
		metadata.initCount++;
		em.flush();
		em.getTransaction().commit();
	}

	@Override
	public StoreMetadata getMetadata() {
		return metadata;
	}
}
