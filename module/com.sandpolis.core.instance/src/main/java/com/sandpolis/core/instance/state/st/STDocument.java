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

import java.util.Collection;
import java.util.function.Consumer;

import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.oid.RelativeOid;

/**
 * {@link STDocument} represents a composite entity and may contain attributes,
 * sub-documents, and sub-collections.
 *
 * @since 5.1.1
 */
public interface STDocument extends STObject<ProtoDocument> {

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

	/**
	 * Get an attribute by its tag. This method never returns {@code null}.
	 *
	 * @param <E> The type of the attribute's value
	 * @param tag The attribute tag
	 * @return The attribute associated with the tag
	 */
	public <E> STAttribute<E> attribute(RelativeOid<STAttribute<E>> oid);

	public default <E> STAttribute<E> attribute(String path) {
		return attribute(new RelativeOid<>(path));
	}

	public int attributeCount();

	/**
	 * Get all attributes in the document.
	 *
	 * @return A collection of all attributes
	 */
	public Collection<STAttribute<?>> attributes();

	/**
	 * Get a document by its relative path, creating any necessary objects along the
	 * way.
	 *
	 * @param oid The relative path to the document
	 * @return The requested document
	 */
	public STDocument document(RelativeOid<STDocument> oid);

	public default STDocument document(String path) {
		return document(new RelativeOid<>(path));
	}

	public int documentCount();

	/**
	 * Get an immutable collection of all {@link STDocument} members.
	 *
	 * @return The requested documents
	 */
	public Collection<STDocument> documents();

	/**
	 * Perform the given action on all {@link STAttribute} members.
	 *
	 * @param consumer The action
	 */
	public void forEachAttribute(Consumer<STAttribute<?>> consumer);

	/**
	 * Perform the given action on all {@link STDocument} members.
	 *
	 * @param consumer The action
	 */
	public void forEachDocument(Consumer<STDocument> consumer);

	public <E> STAttribute<E> getAttribute(RelativeOid<STAttribute<E>> oid);

	/**
	 * Get an attribute by its tag. This method returns {@code null} if the
	 * attribute doesn't exist.
	 *
	 * @param <E> The type of the attribute's value
	 * @param tag The attribute tag
	 * @return The attribute associated with the tag or {@code null}
	 */
	public default <E> STAttribute<E> getAttribute(String path) {
		return attribute(new RelativeOid<>(path));
	}

	public STDocument getDocument(RelativeOid<STDocument> oid);

	/**
	 * Get a document by its relative path. This method returns {@code null} if the
	 * document doesn't exist.
	 *
	 * @param path The relative path to the document
	 * @return The requested document or {@code null}
	 */
	public default STDocument getDocument(String path) {
		return document(new RelativeOid<>(path));
	}

	/**
	 * Remove the given {@link STAttribute} member.
	 *
	 * @param attribute The attribute to remove
	 */
	public void remove(STAttribute<?> attribute);

	/**
	 * Remove the given {@link STDocument} member.
	 *
	 * @param document The document to remove
	 */
	public void remove(STDocument document);

	public default void copyFrom(STDocument other) {
		other.forEachDocument(document -> {
			this.document(document.oid().last()).copyFrom(document);
		});
		other.forEachAttribute(attribute -> {
			this.attribute(attribute.oid().last()).set(attribute.get());
		});
	}
}
