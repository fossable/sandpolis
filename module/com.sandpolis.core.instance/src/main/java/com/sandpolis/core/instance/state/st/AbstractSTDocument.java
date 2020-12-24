//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.instance.state.st;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.instance.state.st.ephemeral.EphemeralAttribute;
import com.sandpolis.core.instance.state.st.ephemeral.EphemeralDocument;

public abstract class AbstractSTDocument extends AbstractSTObject<ProtoDocument> implements STDocument {

	protected BiFunction<STDocument, Oid, STAttribute<?>> attributeConstructor = EphemeralAttribute::new;

	protected Map<String, STAttribute<?>> attributes;

	protected BiFunction<STDocument, Oid, STDocument> documentConstructor = EphemeralDocument::new;

	protected Map<String, STDocument> documents;

	public AbstractSTDocument(STDocument parent, Oid oid) {
		super(parent, oid);
	}

	@Override
	public <E> STAttribute<E> attribute(RelativeOid<STAttribute<E>> oid) {

		if (oid.size() == 1) {
			synchronized (attributes) {
				STAttribute<?> attribute = attributes.get(oid.first());
				if (attribute == null) {
					attribute = attributeConstructor.apply(this, oid.head(1));
					var previous = attributes.put(oid.first(), attribute);

//					if (previous == null) {
//						fireAttributeAddedEvent(this, attribute);
//					}
				}
				return (STAttribute<E>) attribute;
			}
		} else {
			synchronized (documents) {
				STDocument document = documents.get(oid.first());
				if (document == null) {
					document = documentConstructor.apply(this, oid.head(1));
					var previous = documents.put(oid.first(), document);

					if (previous == null) {
						fireDocumentAddedEvent(this, document);
					}
				}
				return document.attribute(oid.tail());
			}
		}
	}

	@Override
	public int attributeCount() {
		return attributes.size();
	}

	@Override
	public Collection<STAttribute<?>> attributes() {
		return Collections.unmodifiableCollection(attributes.values());
	}

	@Override
	public STDocument document(RelativeOid<STDocument> oid) {

		synchronized (documents) {
			STDocument document = documents.get(oid.first());
			if (document == null) {
				document = documentConstructor.apply(this, oid.head(1));
				var previous = documents.put(oid.first(), document);

				if (previous == null) {
					fireDocumentAddedEvent(this, document);
				}
			}
			if (oid.size() == 1) {
				return document;
			} else {
				return document.document(oid.tail());
			}
		}
	}

	@Override
	public int documentCount() {
		return documents.size();
	}

	@Override
	public Collection<STDocument> documents() {
		return Collections.unmodifiableCollection(documents.values());
	}

	@Override
	public void forEachAttribute(Consumer<STAttribute<?>> consumer) {
		attributes.values().forEach(consumer);
	}

	@Override
	public void forEachDocument(Consumer<STDocument> consumer) {
		documents.values().forEach(consumer);
	}

	@Override
	public <E> STAttribute<E> getAttribute(RelativeOid<STAttribute<E>> oid) {
		if (oid.size() == 1) {
			synchronized (attributes) {
				return (STAttribute<E>) attributes.get(oid.first());
			}
		} else {
			synchronized (documents) {
				STDocument document = documents.get(oid.first());
				if (document == null) {
					return null;
				}
				return document.attribute(oid.tail());
			}
		}
	}

	@Override
	public STDocument getDocument(RelativeOid<STDocument> oid) {

		synchronized (documents) {
			STDocument document = documents.get(oid.first());
			if (document == null) {
				return null;
			}
			if (oid.size() == 1) {
				return document;
			} else {
				return document.document(oid.tail());
			}
		}
	}

	@Override
	public void merge(ProtoDocument snapshot) {
		synchronized (documents) {
			for (var document : snapshot.getDocumentList()) {
				if (document.getRemoval()) {
					var removal = documents.remove(document.getPath());
					if (removal != null) {
						fireDocumentRemovedEvent(this, removal);
					}
					continue;
				} else if (document.getReplacement()) {
					documents.remove(document.getPath());
				}
				document(document.getPath()).merge(document);
			}
		}

		synchronized (attributes) {
			for (var attribute : snapshot.getAttributeList()) {
				if (attribute.getRemoval()) {
					attributes.remove(attribute.getPath());
					continue;
				} else if (attribute.getReplacement()) {
					attributes.remove(attribute.getPath());
				}
				attribute(attribute.getPath()).merge(attribute);
			}
		}
	}

	@Override
	public void remove(STAttribute<?> attribute) {
		synchronized (attributes) {
			if (attributes.values().remove(attribute)) {
//				fireAttributeRemovedEvent(this, attribute);
			}
		}
	}

	@Override
	public void remove(STDocument document) {
		synchronized (documents) {
			if (documents.values().remove(document)) {
				fireDocumentRemovedEvent(this, document);
			}
		}
	}

	@Override
	public ProtoDocument snapshot(RelativeOid... oids) {
		var snapshot = ProtoDocument.newBuilder().setPath(oid.last());

		if (oids.length == 0) {
			synchronized (documents) {
				documents.values().stream().map(STDocument::snapshot).forEach(snapshot::addDocument);
			}
			synchronized (attributes) {
				attributes.values().stream().map(STAttribute::snapshot).forEach(snapshot::addAttribute);
			}
		} else {
			for (var head : Arrays.stream(oids).map(Oid::first).distinct().toArray()) {
				var children = Arrays.stream(oids).filter(oid -> oid.first() != head).map(Oid::tail)
						.toArray(RelativeOid[]::new);

				if (documents.containsKey(head))
					snapshot.addDocument(documents.get(head).snapshot(children));
				if (attributes.containsKey(head))
					snapshot.addAttribute(attributes.get(head).snapshot());
			}
		}

		return snapshot.build();
	}
}
