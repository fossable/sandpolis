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
package com.sandpolis.core.instance.state;

import java.util.function.Function;

import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.state.container.DocumentContainer;
import com.sandpolis.core.instance.store.StoreMetadata;

/**
 * A {@link STCollection} is an unordered set of documents. Every document has a
 * unique non-zero "tag" which is a function of the document's identity.
 *
 * @since 5.1.1
 */
public interface STCollection extends STObject<ProtoCollection>, DocumentContainer {

	/**
	 * Indicates that an {@link STDocument} has been added to the collection.
	 */
	public static final class DocumentAddedEvent {
		public final STCollection collection;
		public final STDocument newDocument;

		public DocumentAddedEvent(STCollection collection, STDocument newDocument) {
			this.collection = collection;
			this.newDocument = newDocument;
		}
	}

	/**
	 * Indicates that an {@link STDocument} has been removed from the collection.
	 */
	public static final class DocumentRemovedEvent {
		public final STCollection collection;
		public final STDocument oldDocument;

		public DocumentRemovedEvent(STCollection collection, STDocument oldDocument) {
			this.collection = collection;
			this.oldDocument = oldDocument;
		}
	}

	public <E extends VirtObject> STRelation<E> collectionList(Function<STDocument, E> constructor);

	public StoreMetadata getMetadata();

	/**
	 * Returns the number of elements in this collection.
	 *
	 * @return The number of elements in this collection
	 */
	public int size();
}
