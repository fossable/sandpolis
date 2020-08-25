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

import java.util.stream.Stream;

import com.sandpolis.core.instance.State.ProtoDocument;

/**
 * {@link STDocument} represents a composite entity and may contain attributes,
 * sub-documents, and sub-collections.
 *
 * @since 5.1.1
 */
public interface STDocument extends STObject<ProtoDocument> {

	/**
	 * Get an attribute by its tag. This method never returns {@code null}.
	 * 
	 * @param <E> The type of the attribute's value
	 * @param tag The attribute tag
	 * @return The attribute associated with the tag
	 */
	public <E> STAttribute<E> attribute(int tag);

	/**
	 * Get all attributes in the document that currently have a value.
	 * 
	 * @return A stream of all attributes
	 */
	public Stream<STAttribute<?>> attributes();

	/**
	 * Get an attribute by its tag. This method returns {@code null} if the
	 * attribute doesn't exist.
	 * 
	 * @param <E> The type of the attribute's value
	 * @param tag The attribute tag
	 * @return The attribute associated with the tag or {@code null}
	 */
	public <E> STAttribute<E> getAttribute(int tag);

	/**
	 * Overwrite the attribute associated with the given tag.
	 * 
	 * @param tag       The attribute tag
	 * @param attribute The attribute to associate with the tag or {@code null}
	 */
	public void setAttribute(int tag, STAttribute<?> attribute);

	/**
	 * Get a subcollection by its tag. This method never returns {@code null}.
	 * 
	 * @param tag The subcollection tag
	 * @return The subcollection associated with the tag
	 */
	public STCollection collection(int tag);

	/**
	 * Get all subcollections.
	 * 
	 * @return A stream of all subcollections
	 */
	public Stream<STCollection> collections();

	/**
	 * Get a subcollection by its tag. This method returns {@code null} if the
	 * subcollection doesn't exist.
	 * 
	 * @param tag The subcollection tag
	 * @return The subcollection associated with the tag or {@code null}
	 */
	public STCollection getCollection(int tag);

	/**
	 * Overwrite the subcollection associated with the given tag.
	 * 
	 * @param tag        The subcollection tag
	 * @param collection The subcollection to associate with the tag or {@code null}
	 */
	public void setCollection(int tag, STCollection collection);

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
	public Stream<STCollection> documents();

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

	public String getId();

	/**
	 * Get the document's oid.
	 * 
	 * @return The document's oid
	 */
	public Oid<?> getOid();

	public void setOid(Oid<?> oid);
}
