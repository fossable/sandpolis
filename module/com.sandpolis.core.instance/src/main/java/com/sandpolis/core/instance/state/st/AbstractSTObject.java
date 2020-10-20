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

import static com.sandpolis.core.instance.state.STStore.STStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.protobuf.Message;
import com.sandpolis.core.instance.state.oid.AbsoluteOid;
import com.sandpolis.core.instance.state.oid.Oid;

public abstract class AbstractSTObject<E extends Message> implements STObject<E> {

	private static final Logger log = LoggerFactory.getLogger(AbstractSTObject.class);

	private EventBus bus;

	/**
	 * The number of listeners registered to the {@link #bus}.
	 */
	private int listeners;

	protected Oid oid;

	protected long tag;

	@Override
	public boolean isAttached() {
		return tag != 0;
	}

	public synchronized void addListener(Object listener) {
		if (bus == null) {
			bus = new EventBus();
		}
		bus.register(listener);
		listeners++;
	}

	public long getTag() {
		return tag;
	}

	public Oid oid() {
		if (!isAttached())
			return null;

		if (parent() == null) {
			return AbsoluteOid.newOid(tag);
		}

		var parentOid = parent().oid();
		if (parentOid == null) {
			return AbsoluteOid.newOid(tag);
		}

		return parentOid.child(tag);
	}

	public abstract AbstractSTObject<?> parent();

	public synchronized void removeListener(Object listener) {
		if (bus != null) {
			bus.unregister(listener);
			listeners--;
		}
		if (listeners == 0) {
			bus = null;
		}
	}

	public void setTag(long tag) {
		this.tag = tag;
	}

	protected synchronized <T> void fireAttributeValueChangedEvent(STAttribute<T> attribute,
			STAttributeValue<T> oldValue, STAttributeValue<T> newValue) {
		if (!isAttached())
			return;

		if (log.isTraceEnabled() && attribute == this) {
			log.trace("Attribute ({}) changed value from \"{}\" to \"{}\"", attribute.oid(), oldValue, newValue);
		}

		if (bus != null) {
			STStore.pool().submit(() -> {
				bus.post(new STAttribute.ChangeEvent<T>(attribute, oldValue, newValue));
			});
		}

		if (parent() != null)
			parent().fireAttributeValueChangedEvent(attribute, oldValue, newValue);
	}

	protected synchronized void fireDocumentAddedEvent(STCollection collection, STDocument newDocument) {
		if (!isAttached())
			return;

		if (log.isTraceEnabled() && collection == this) {
			log.trace("Document ({}) added to collection ({})", newDocument.oid().last(), collection.oid());
		}

		if (bus != null) {
			STStore.pool().submit(() -> {
				bus.post(new STCollection.DocumentAddedEvent(collection, newDocument));
			});
		}

		if (parent() != null)
			parent().fireDocumentAddedEvent(collection, newDocument);
	}

	protected synchronized void fireDocumentRemovedEvent(STCollection collection, STDocument oldDocument) {
		if (!isAttached())
			return;

		if (log.isTraceEnabled() && collection == this) {
			log.trace("Document ({}) removed from collection ({})", oldDocument.oid().last(), collection.oid());
		}

		if (bus != null) {
			STStore.pool().submit(() -> {
				bus.post(new STCollection.DocumentRemovedEvent(collection, oldDocument));
			});
		}

		if (parent() != null)
			parent().fireDocumentRemovedEvent(collection, oldDocument);
	}

	protected synchronized void fireDocumentAddedEvent(STDocument document, STDocument newDocument) {
		if (!isAttached())
			return;

		if (log.isTraceEnabled() && document == this) {
			log.trace("Document ({}) added to document ({})", newDocument.oid().last(), document.oid());
		}

		if (bus != null) {
			STStore.pool().submit(() -> {
				bus.post(new STDocument.DocumentAddedEvent(document, newDocument));
			});
		}

		if (parent() != null)
			parent().fireDocumentAddedEvent(document, newDocument);
	}

	protected synchronized void fireDocumentRemovedEvent(STDocument document, STDocument oldDocument) {
		if (!isAttached())
			return;

		if (log.isTraceEnabled() && document == this) {
			log.trace("Document ({}) removed from document ({})", oldDocument.oid().last(), document.oid());
		}

		if (bus != null) {
			STStore.pool().submit(() -> {
				bus.post(new STDocument.DocumentRemovedEvent(document, oldDocument));
			});
		}

		if (parent() != null)
			parent().fireDocumentRemovedEvent(document, oldDocument);
	}

	protected synchronized void fireCollectionAddedEvent(STDocument document, STCollection newCollection) {
		if (!isAttached())
			return;

		if (log.isTraceEnabled() && document == this) {
			log.trace("Collection ({}) added to document ({})", newCollection.oid().last(), document.oid());
		}

		if (bus != null) {
			STStore.pool().submit(() -> {
				bus.post(new STDocument.CollectionAddedEvent(document, newCollection));
			});
		}

		if (parent() != null)
			parent().fireCollectionAddedEvent(document, newCollection);
	}

	protected synchronized void fireCollectionRemovedEvent(STDocument document, STCollection oldCollection) {
		if (!isAttached())
			return;

		if (log.isTraceEnabled() && document == this) {
			log.trace("Collection ({}) removed from document ({})", oldCollection.oid().last(), document.oid());
		}

		if (bus != null) {
			STStore.pool().submit(() -> {
				bus.post(new STDocument.CollectionRemovedEvent(document, oldCollection));
			});
		}

		if (parent() != null)
			parent().fireCollectionRemovedEvent(document, oldCollection);
	}
}
