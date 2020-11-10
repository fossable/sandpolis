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

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;

import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.state.vst.VirtCollection;
import com.sandpolis.core.instance.state.vst.VirtDocument;

/**
 * {@link STCollectionStore} is a store backed by an {@link STDocument} which
 * may exist exclusively in memory (ephemeral collection), in a database
 * (hibernate collection), or on another instance across the network (entangled
 * collection).
 *
 * @param <V>
 */
public abstract class STCollectionStore<V extends VirtDocument> extends StoreBase
		implements MetadataStore<StoreMetadata> {

	protected VirtCollection<? super V> collection;

	protected STCollectionStore(Logger log) {
		super(log);
	}

	protected V add(Function<STDocument, V> constructor) {
//		return collection.add(constructor);
		return null;
	}

	/**
	 * Determine how many elements the store contains.
	 *
	 * @return The number of elements in the store
	 */
	public long count() {
		return collection.count();
	}

	public Optional<V> get(String path) {
		return (Optional<V>) collection.get(path);
	}

	public Optional<V> remove(String path) {
		var item = get(path);
		if (item.isPresent())
			removeValue(item.get());
		return item;
	}

	public void removeValue(V value) {
	}

	public Collection<V> values() {
		return null;
	}

	@Override
	public StoreMetadata getMetadata() {
		return null;// collection.getMetadata();
	}

}
