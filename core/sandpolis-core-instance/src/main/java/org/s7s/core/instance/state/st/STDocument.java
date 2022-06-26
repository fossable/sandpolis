//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.state.st;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.s7s.core.protocol.Stream.EV_STStreamData;
import org.s7s.core.instance.state.oid.Oid;

/**
 * {@link STDocument} is a composite entity that may contain attributes and
 * sub-documents.
 *
 * @since 5.1.1
 */
public interface STDocument extends STObject {

	/**
	 * Indicates that an {@link STDocument} has been added to the document.
	 */
	public static final record DocumentAddedEvent(STDocument document, STDocument newDocument) {
	}

	/**
	 * Indicates that an {@link STDocument} has been removed from the document.
	 */
	public static final record DocumentRemovedEvent(STDocument document, STDocument oldDocument) {
	}

	/**
	 * Retrieve or create an attribute at the given OID. Any intermediate documents
	 * will be created if necessary.
	 *
	 * @param oid An OID which must be a descendant of this document's OID
	 * @return A new or old attribute
	 */
	public default STAttribute attribute(Oid oid) {
		if (!oid().isAncestorOf(path)) {
			throw new IllegalArgumentException("/" + Arrays.stream(path).collect(Collectors.joining("/"))
					+ " is not a descendant of: " + oid().toString());
		}
		if ((path.length - oid().path().length) == 1) {
			return attribute(path[path.length - 1]);
		}

		STDocument document = this;
		for (int i = oid().path().length; i < path.length - 1; i++) {
			document = document.document(path[i]);
		}

		return document.attribute(path[path.length - 1]);
	}

	/**
	 * @param id The ID of the child to retrieve or create
	 * @return A child attribute with the given ID
	 */
	public STAttribute attribute(String id);

	/**
	 * @return The number of sub-attributes belonging to this document
	 */
	public int attributeCount();

	/*
	 * public default void copyFrom(STDocument other) {
	 * other.forEachDocument(document -> {
	 * this.document(document.oid().last()).copyFrom(document); });
	 * other.forEachAttribute(attribute -> {
	 * this.attribute(attribute.oid().last()).set(attribute.get()); }); }
	 */

	public default STDocument document(Oid oid) {
		STDocument document = this;
		for (int i = oid().path().length; i < path.length; i++) {
			document = document.document(path[i]);
		}

		return document;
	}

	public STDocument document(String id);

	public STDocument getDocument(String id);

	public STAttribute getAttribute(String id);

	/**
	 * @return The number of sub-documents belonging to this document
	 */
	public int documentCount();

	/**
	 * Perform the given action on all {@link STAttribute} members.
	 *
	 * @param consumer The action
	 */
	public void forEachAttribute(Consumer<STAttribute> consumer);

	/**
	 * Perform the given action on all {@link STDocument} members.
	 *
	 * @param consumer The action
	 */
	public void forEachDocument(Consumer<STDocument> consumer);

	/**
	 * Remove the given {@link STAttribute} member.
	 *
	 * @param attribute The attribute to remove
	 */
	public void remove(STAttribute attribute);

	/**
	 * Remove the given {@link STDocument} member.
	 *
	 * @param document The document to remove
	 */
	public void remove(STDocument document);

	/**
	 * Remove the given {@link STDocument} or {@link STAttribute} member by id.
	 *
	 * @param id The item to remove
	 */
	public void remove(String id);

	public void set(String id, STAttribute attribute);

	public void set(String id, STDocument document);

	public default String getId() {
		return oid().last();
	}

	@Override
	public default void merge(EV_STStreamData snapshot) {

		if (snapshot.getRemoved()) {
			STDocument document = this;
			String[] path = snapshot.getOid().split("/");

			for (int i = 0; i < path.length - 1; i++) {
				document = document.document(path[i]);
			}

			document.remove(path[path.length - 1]);
		} else {
			attribute(oid().relative(snapshot.getOid())).merge(snapshot);
		}
	}

	@Override
	public default Stream<EV_STStreamData> snapshot(STSnapshotStruct config) {

		List<Stream<EV_STStreamData>> streams = new ArrayList<>();

		if (config.whitelist.size() == 0) {
			forEachDocument(document -> {
				streams.add(document.snapshot(config));
			});
			forEachAttribute(attribute -> {
				streams.add(attribute.snapshot(config));
			});
		} else {
//			for (var head : Arrays.stream(oids).map(Oid::first).distinct().toArray(String[]::new)) {
//				var children = Arrays.stream(oids).filter(oid -> oid.first() != head).toArray(Oid[]::new);
//
//				var document = getDocument(head);
//				if (document != null) {
//					streams.add(document.snapshot(children));
//				}
//
//				var attribute = getAttribute(head);
//				if (attribute != null) {
//					streams.add(attribute.snapshot(config));
//				}
//			}
		}

		return streams.stream().flatMap(Function.identity());
	}
}
