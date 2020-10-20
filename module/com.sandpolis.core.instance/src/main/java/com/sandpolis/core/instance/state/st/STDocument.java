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
package com.sandpolis.core.instance.state.st;

import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.oid.AbsoluteOid;

/**
 * {@link STDocument} represents a composite entity and may contain attributes,
 * sub-documents, and sub-collections.
 *
 * @since 5.1.1
 */
public interface STDocument
		extends STObject<ProtoDocument>, STAttributeContainer, STDocumentContainer, STCollectionContainer {

	/**
	 * Indicates that an {@link STCollection} has been added to the document.
	 */
	public static final class CollectionAddedEvent {
		public final STDocument document;
		public final STCollection newCollection;

		public CollectionAddedEvent(STDocument document, STCollection newCollection) {
			this.document = document;
			this.newCollection = newCollection;
		}
	}

	/**
	 * Indicates that an {@link STCollection} has been removed from the document.
	 */
	public static final class CollectionRemovedEvent {
		public final STDocument document;
		public final STCollection oldCollection;

		public CollectionRemovedEvent(STDocument document, STCollection oldCollection) {
			this.document = document;
			this.oldCollection = oldCollection;
		}
	}

	/**
	 * Indicates that an {@link STDocument} has been added to the document.
	 */
	public static final class DocumentAddedEvent {
		public final STDocument document;
		public final STDocument newDocument;

		public DocumentAddedEvent(STDocument document, STDocument newDocument) {
			this.document = document;
			this.newDocument = newDocument;
		}
	}

	/**
	 * Indicates that an {@link STDocument} has been removed from the document.
	 */
	public static final class DocumentRemovedEvent {
		public final STDocument document;
		public final STDocument oldDocument;

		public DocumentRemovedEvent(STDocument document, STDocument oldDocument) {
			this.document = document;
			this.oldDocument = oldDocument;
		}
	}

	public default <T> STAttribute<T> get(AbsoluteOid.STAttributeOid<T> oid) {
		return get(oid.relativize(oid()));
	}

	public default STCollection get(AbsoluteOid.STCollectionOid<?> oid) {
		return get(oid.relativize(oid()));
	}

	public default STDocument get(AbsoluteOid.STDocumentOid<?> oid) {
		return get(oid.relativize(oid()));
	}

	public String getId();

}
