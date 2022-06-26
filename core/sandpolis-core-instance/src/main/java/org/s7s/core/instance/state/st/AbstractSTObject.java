//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.state.st;

import static org.s7s.core.instance.state.STStore.STStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import org.s7s.core.instance.state.oid.Oid;
import org.s7s.core.instance.state.st.EphemeralAttribute.EphemeralAttributeValue;

public abstract class AbstractSTObject implements STObject {

	private static final Logger log = LoggerFactory.getLogger(AbstractSTObject.class);

	/**
	 * The event bus that delivers change events. It is only initialized when a
	 * listener is attached. If the bus does not exist, events will not be
	 * generated.
	 */
	private EventBus bus;

	private final String id;

	/**
	 * The number of listeners registered to the {@link #bus}.
	 */
	private int listeners;

	protected AbstractSTObject parent;

	public AbstractSTObject(STDocument parent, String id) {
		this.parent = (AbstractSTObject) parent;
		this.id = id;
	}

	@Override
	public synchronized void addListener(Object listener) {
		if (bus == null) {
			bus = new EventBus();
		}
		bus.register(listener);
		listeners++;
	}

	protected synchronized void fireAttributeValueChangedEvent(STAttribute attribute, EphemeralAttributeValue oldValue,
			EphemeralAttributeValue newValue) {

		if (log.isTraceEnabled() && attribute == this) {
			log.trace("Attribute ({}) changed value from \"{}\" to \"{}\"", attribute.oid(), oldValue, newValue);
		}

		if (bus != null) {
			STStore.pool().submit(() -> {
				bus.post(new STAttribute.ChangeEvent(attribute, oldValue, newValue));
			});
		}

		if (parent != null)
			parent.fireAttributeValueChangedEvent(attribute, oldValue, newValue);
	}

	protected synchronized void fireDocumentAddedEvent(STDocument document, STDocument newDocument) {

		if (log.isTraceEnabled() && document == this) {
			log.trace("Document ({}) added to document ({})", newDocument.oid().last(), document.oid());
		}

		if (bus != null) {
			STStore.pool().submit(() -> {
				bus.post(new STDocument.DocumentAddedEvent(document, newDocument));
			});
		}

		if (parent != null)
			parent.fireDocumentAddedEvent(document, newDocument);
	}

	protected synchronized void fireDocumentRemovedEvent(STDocument document, STDocument oldDocument) {

		if (log.isTraceEnabled() && document == this) {
			log.trace("Document ({}) removed from document ({})", oldDocument.oid().last(), document.oid());
		}

		if (bus != null) {
			STStore.pool().submit(() -> {
				bus.post(new STDocument.DocumentRemovedEvent(document, oldDocument));
			});
		}

		if (parent != null)
			parent.fireDocumentRemovedEvent(document, oldDocument);
	}

	@Override
	public Oid oid() {
		if (parent == null) {
			if (id == null) {
				return Oid.of("/");
			} else {
				return Oid.of("/").child(id);
			}
		}
		return parent.oid().child(id);
	}

	@Override
	public STDocument parent() {
		return (STDocument) parent;
	}

	@Override
	public synchronized void removeListener(Object listener) {
		if (bus != null) {
			bus.unregister(listener);
			listeners--;
		}
		if (listeners == 0) {
			bus = null;
		}
	}

	@Override
	public void replaceParent(STDocument parent) {
		this.parent = (AbstractSTObject) parent;
	}
}
