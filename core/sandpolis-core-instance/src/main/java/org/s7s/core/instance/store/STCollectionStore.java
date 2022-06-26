//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.state.vst.AbstractSTDomainObject;
import org.slf4j.Logger;

import com.google.common.cache.Cache;

/**
 * {@link STCollectionStore} is a store backed by an {@link STDocument} which
 * may exist exclusively in memory (ephemeral collection), in a database, or on
 * another instance across the network (entangled collection).
 *
 * @param <V>
 */
public abstract class STCollectionStore<V extends AbstractSTDomainObject> extends StoreBase {

	private Cache<String, V> cache;

	protected STDocument collection;

	private final Function<STDocument, V> constructor;

	protected STCollectionStore(Logger log, Function<STDocument, V> constructor, STDocument collection) {
		super(log);
		this.constructor = constructor;
		this.documents = new HashMap<>();

		collection.forEachDocument(document -> {
			documents.put(document.getId(), constructor.apply(document));
		});
	}

	/**
	 * Determine how many elements the store contains.
	 *
	 * @return The number of elements in the store
	 */
	public long count() {
		return collection.documentCount();
	}

	public Optional<V> get(String id) {
		return Optional.ofNullable(cache.get(id, () -> {
			var d = collection.getDocument(id);
			if (d != null) {
				return constructor.apply(d);
			}
			return null;
		}));
	}

	public Optional<V> remove(String id) {
		var item = cache.getIfPresent(id);
		cache.invalidate(id);
		if (item == null) {
			var d = collection.getDocument(id);
			if (d != null) {
				item = constructor.apply(d);
			}
		}
		collection.remove(id);
		return Optional.ofNullable(item);
	}

	public void removeValue(V value) {
		cache.asMap().values().remove(value);
		// TODO remove from collection
	}

	public Collection<V> values() {
		return documents.values();
	}

	public void add(V value) {
		String id = value.getId();
		if (id == null) {
			id = UUID.randomUUID().toString();
		}
		cache.put(id, value);
	}

	public V create(Consumer<AbstractSTDomainObject> configurator) {
		String id = UUID.randomUUID().toString();
		V object = constructor.apply(collection.document(id));
		configurator.accept(object);
		documents.put(id, object);
		return object;
	}
}
