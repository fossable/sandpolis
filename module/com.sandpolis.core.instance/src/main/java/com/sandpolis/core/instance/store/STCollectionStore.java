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

import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.sandpolis.core.foundation.Result.ErrorCode;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.VirtObject;

public abstract class STCollectionStore<V extends VirtObject> extends StoreBase
		implements MetadataStore<StoreMetadata> {

	protected STCollection collection;

	protected STCollectionStore(Logger log) {
		super(log);
	}

	protected abstract V constructor(STDocument document);

	protected void add(VirtObject object) {
		if (object.complete() != ErrorCode.OK) {
			// TODO
			throw new RuntimeException();
		}
		collection.setDocument(object.tag(), object.document);
	}

	/**
	 * Determine how many elements the store contains.
	 *
	 * @return The number of elements in the store
	 */
	public long count() {
		return collection.size();
	}

	public Optional<V> get(int tag) {
		var document = collection.getDocument(tag);
		if (document == null) {
			return Optional.empty();
		} else {
			return Optional.of(constructor(document));
		}
	}

	public Optional<V> remove(int tag) {
		var item = get(tag);
		if (item.isPresent())
			removeValue(item.get());
		return item;
	}

	public void removeValue(V value) {

	}

	public Stream<V> stream() {
		return collection.documents().map(this::constructor);
	}

	@Override
	public StoreMetadata getMetadata() {
		return collection.getMetadata();
	}
}
