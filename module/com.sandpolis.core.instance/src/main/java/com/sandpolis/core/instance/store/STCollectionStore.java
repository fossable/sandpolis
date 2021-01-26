//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.instance.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;

import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.state.vst.AbstractSTDomainObject;

/**
 * {@link STCollectionStore} is a store backed by an {@link STDocument} which
 * may exist exclusively in memory (ephemeral collection), in a database
 * (hibernate collection), or on another instance across the network (entangled
 * collection).
 *
 * @param <V>
 */
public abstract class STCollectionStore<V extends AbstractSTDomainObject> extends StoreBase
		implements MetadataStore<StoreMetadata> {

	private Map<String, V> documents;

	private STDocument collection;

	private final Function<STDocument, V> constructor;

	protected STCollectionStore(Logger log, Function<STDocument, V> constructor) {
		super(log);
		this.constructor = constructor;
		this.documents = new HashMap<>();
	}

	/**
	 * Determine how many elements the store contains.
	 *
	 * @return The number of elements in the store
	 */
	public long count() {
		return documents.size();
	}

	public Optional<V> get(String path) {
		return Optional.ofNullable(documents.get(path));
	}

	public Optional<V> remove(String path) {
		var item = get(path);
		if (item.isPresent())
			removeValue(item.get());
		return item;
	}

	public void removeValue(V value) {
	}

	public void setDocument(STDocument collection) {
		this.collection = collection;
		this.documents.clear();

		collection.forEachDocument(document -> {
			documents.put(null, constructor.apply(document));
		});
	}

	public Collection<V> values() {
		return documents.values();
	}

	public V create(Consumer<AbstractSTDomainObject> configurator) {
		String id = UUID.randomUUID().toString();
		V object = constructor.apply(collection.document(id));
		configurator.accept(object);
		documents.put(id, object);
		return object;
	}

	@Override
	public StoreMetadata getMetadata() {
		// TODO
		return new StoreMetadata() {

			@Override
			public int getInitCount() {
				// TODO Auto-generated method stub
				return 1;
			}
		};
	}

}
