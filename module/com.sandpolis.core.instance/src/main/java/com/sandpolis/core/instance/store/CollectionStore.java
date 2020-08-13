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
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.sandpolis.core.instance.state.VirtObject;
import com.sandpolis.core.instance.store.provider.StoreProvider;

public abstract class CollectionStore<V> extends StoreBase implements MetadataStore<StoreMetadata> {

	protected StoreProvider<V> provider;

	protected CollectionStore(Logger log) {
		super(log);
	}

	protected V add(V object, Consumer<V> configurator) {
		configurator.accept(object);
		if (object instanceof VirtObject) {
			var virtObject = (VirtObject) object;
			if (!virtObject.checkIdentity()) {
				throw new IllegalArgumentException("Cannot add object with undefined identity attributes");
			}

			// Attach OID
			if (provider.getOid() != null)
				virtObject.document.setOid(provider.getOid().child(virtObject.tag()));
		}

		provider.add(object);
		return object;
	}

	@Override
	public void close() throws Exception {
		provider = null;
	}

	/**
	 * Determine how many elements the store contains.
	 *
	 * @return The number of elements in the store
	 */
	public long count() {
		return provider.count();
	}

	public Optional<V> get(int tag) {
		return provider.get(tag);
	}

	public Optional<V> remove(int tag) {
		var item = get(tag);
		if (item.isPresent())
			removeValue(item.get());
		return item;
	}

	public void removeValue(V value) {
		provider.remove(value);
	}

	public Stream<V> stream() {
		return provider.stream();
	}

	@Override
	public StoreMetadata getMetadata() {
		return provider.getMetadata();
	}
}
