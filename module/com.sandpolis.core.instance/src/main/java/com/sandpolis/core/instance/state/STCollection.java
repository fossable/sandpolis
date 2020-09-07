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
import java.util.stream.Stream;

import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.state.oid.OidBase;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.instance.store.StoreMetadata;

/**
 * A {@link STCollection} is an unordered set of documents. Every document has a
 * unique non-zero "tag" which is a function of the document's identity.
 *
 * @since 5.1.1
 */
public interface STCollection extends STObject<ProtoCollection> {

	/**
	 * Get a subdocument by its tag. This method never returns {@code null}.
	 *
	 * @param tag The subdocument tag
	 * @return The subdocument associated with the tag
	 */
	public STDocument document(int tag);

	/**
	 * Get all subdocuments.
	 *
	 * @return A stream of all subdocuments
	 */
	public Stream<STDocument> documents();

	/**
	 * Get a subdocument by its tag. This method returns {@code null} if the
	 * subdocument doesn't exist.
	 *
	 * @param tag The subdocument tag
	 * @return The subdocument associated with the tag or {@code null}
	 */
	public STDocument getDocument(int tag);

	/**
	 * Overwrite the attribute associated with the given tag.
	 *
	 * @param tag      The attribute tag
	 * @param document The attribute to associate with the tag or {@code null}
	 */
	public void setDocument(int tag, STDocument document);

	/**
	 * Returns the number of elements in this collection.
	 *
	 * @return The number of elements in this collection
	 */
	public int size();

	public default <E> STAttribute<E> attribute(RelativeOid<E> oid) {
		if (!oid.isConcrete())
			throw new RuntimeException();

		switch (oid.first() % 10) {
		case OidBase.SUFFIX_DOCUMENT:
			return (STAttribute<E>) document(oid.first()).attribute(oid.tail());
		default:
			throw new RuntimeException("Unacceptable attribute tag: " + oid.first());
		}
	}

	public default STDocument document(RelativeOid<?> oid) {
		if (!oid.isConcrete())
			throw new RuntimeException();

		switch (oid.first() % 10) {
		case OidBase.SUFFIX_DOCUMENT:
			if (oid.size() == 1) {
				return document(oid.first());
			} else {
				return document(oid.first()).document(oid.tail());
			}
		default:
			throw new RuntimeException("Unacceptable document tag: " + oid.first());
		}
	}

	public default STCollection collection(RelativeOid<?> oid) {
		if (!oid.isConcrete())
			throw new RuntimeException();

		switch (oid.first() % 10) {
		case OidBase.SUFFIX_DOCUMENT:
			return document(oid.first()).collection(oid.tail());
		default:
			throw new RuntimeException("Unacceptable attribute tag: " + oid.first());
		}
	}

	public <E extends VirtObject> STRelation<E> collectionList(Function<STDocument, E> constructor);

	public STDocument newDocument();

	public StoreMetadata getMetadata();

	public void remove(STDocument document);
}
