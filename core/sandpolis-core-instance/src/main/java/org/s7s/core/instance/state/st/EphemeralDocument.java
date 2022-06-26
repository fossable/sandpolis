//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.state.st;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class EphemeralDocument extends AbstractSTObject implements STDocument {

	private final Map<String, STAttribute> attributes;

	private final Map<String, STDocument> documents;

	public EphemeralDocument(STDocument parent, String id) {
		super(parent, id);
		this.attributes = new HashMap<>();
		this.documents = new HashMap<>();
	}

	@Override
	public int attributeCount() {
		return attributes.size();
	}

	@Override
	public STDocument document(String id) {
		synchronized (documents) {
			STDocument document = documents.get(id);
			if (document == null) {
				document = new EphemeralDocument(this, id);
				documents.put(id, document);
				fireDocumentAddedEvent(this, document);
			}
			return document;
		}
	}

	@Override
	public STAttribute attribute(String id) {
		synchronized (attributes) {
			STAttribute attribute = attributes.get(id);
			if (attribute == null) {
				attribute = new EphemeralAttribute(this, id);
				attributes.put(id, attribute);
			}
			return attribute;
		}
	}

	@Override
	public int documentCount() {
		return documents.size();
	}

	@Override
	public void forEachAttribute(Consumer<STAttribute> consumer) {
		synchronized (attributes) {
			attributes.values().forEach(consumer);
		}
	}

	@Override
	public void forEachDocument(Consumer<STDocument> consumer) {
		synchronized (documents) {
			documents.values().forEach(consumer);
		}
	}

	@Override
	public void remove(STAttribute attribute) {
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
	public void remove(String id) {
		synchronized (documents) {
			if (documents.remove(id) != null) {
				return;
			}
		}
		synchronized (attributes) {
			if (attributes.remove(id) != null) {
				return;
			}
		}
	}

	@Override
	public void set(String id, STAttribute attribute) {
		synchronized (attributes) {
			var previous = attributes.put(id, attribute);
			attribute.replaceParent(this);
		}
	}

	@Override
	public void set(String id, STDocument document) {
		synchronized (documents) {
			var previous = documents.put(id, document);
			document.replaceParent(this);
		}
	}

	@Override
	public STDocument getDocument(String id) {
		return documents.get(id);
	}

	@Override
	public STAttribute getAttribute(String id) {
		return attributes.get(id);
	}
}
