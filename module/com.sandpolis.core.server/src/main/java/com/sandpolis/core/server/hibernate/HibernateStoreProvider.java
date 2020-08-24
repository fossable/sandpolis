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
package com.sandpolis.core.server.hibernate;

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

import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.VirtObject;
import com.sandpolis.core.instance.store.StoreMetadata;
import com.sandpolis.core.instance.store.provider.StoreProvider;
import com.sandpolis.core.server.state.ServerCollection;
import com.sandpolis.core.server.state.ServerDocument;

/**
 * {@link HibernateStoreProvider} is a persistent {@link StoreProvider} backed
 * by a Hibernate connection. It is responsible for exactly one
 * {@link ServerCollection}.
 * 
 * <p>
 * This object is persisted along with its data.
 *
 * @since 5.0.0
 */
@Entity
public class HibernateStoreProvider<E extends VirtObject> implements StoreProvider<E> {

	/**
	 * The identifier is the fully qualified class name of the object that the
	 * provider manages.
	 */
	@Id
	private String id;

	@OneToOne(cascade = CascadeType.ALL)
	private ServerCollection collection;

	@OneToOne(cascade = CascadeType.ALL)
	private HibernateStoreProviderMetadata metadata;

	@Transient
	EntityManager em;

	@Transient
	Function<STDocument, E> constructor;

	@Transient
	Class<E> type;

	public HibernateStoreProvider(Class<E> type, Oid<?> oid) {
		this.type = type;
		this.id = type.getName();
		this.collection = new ServerCollection(oid);
		this.metadata = new HibernateStoreProviderMetadata();
	}

	HibernateStoreProvider() {
		// JPA constructor
	}

	@Override
	public void initialize() {
		em.getTransaction().begin();
		metadata.initCount++;
		em.flush();
		em.getTransaction().commit();
	}

	@Override
	public synchronized void add(E e) {
		em.getTransaction().begin();
		collection.add(e.tag(), (ServerDocument) e.document);
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
		collection.remove(e.document);
		em.flush();
		em.getTransaction().commit();
	}

	@Override
	public synchronized void removeIf(Predicate<E> condition) {
		em.getTransaction().begin();
		stream().filter(condition).map(e -> e.document).forEach(collection::remove);
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
	public Oid<?> getOid() {
		return collection.getOid();
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
	public StoreMetadata getMetadata() {
		return metadata;
	}
}
