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
package com.sandpolis.core.instance.store;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.google.common.collect.Streams;
import com.sandpolis.core.foundation.Result.ErrorCode;
import com.sandpolis.core.instance.state.st.STCollection;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.state.vst.VirtObject;
import com.sandpolis.core.instance.state.vst.VirtObject.IncompleteObjectException;

/**
 * {@link STCollectionStore} is a store backed by a {@link STCollection} which
 * may exist exclusively in memory (ephemeral collection), in a database
 * (hibernate collection), or on another instance across the network (entangled
 * collection).
 *
 * @param <V>
 */
public abstract class STCollectionStore<V extends VirtObject> extends StoreBase
		implements MetadataStore<StoreMetadata> {

	protected STCollection collection;

	/**
	 * This top-level cache allows {@link VirtObject}s to store transient state as
	 * regular fields.
	 */
	private Map<Long, V> cache = new HashMap<>();

	protected STCollectionStore(Logger log) {
		super(log);
	}

	/**
	 * A function that translates an ST object into a VST object.
	 *
	 * @param document An ST object
	 * @return A new VST object
	 */
	protected abstract V constructor(STDocument document);

	protected void add(V object) {
		if (object.complete() != ErrorCode.OK)
			throw new IncompleteObjectException();

		long tag = object.tag();
		collection.setDocument(tag, object.document);
		cache.put(tag, object);
	}

	/**
	 * Determine how many elements the store contains.
	 *
	 * @return The number of elements in the store
	 */
	public long count() {
		return collection.size();
	}

	public Optional<V> get(long tag) {
		var object = cache.get(tag);
		if (object != null)
			return Optional.of(object);

		var document = collection.getDocument(tag);
		if (document == null) {
			return Optional.empty();
		} else {
			return Optional.of(constructor(document));
		}
	}

	public Optional<V> remove(long tag) {
		var item = get(tag);
		if (item.isPresent())
			removeValue(item.get());
		return item;
	}

	public void removeValue(V value) {
		cache.values().remove(value);
		collection.remove(value.document);
	}

	public Stream<V> stream() {
		if (cache.size() == 0)
			return collection.documents().stream().map(this::constructor);

		return Streams.concat(cache.values().stream(), collection.documents().stream().map(this::constructor))
				.distinct();
	}

	@Override
	public StoreMetadata getMetadata() {
		return collection.getMetadata();
	}
}
