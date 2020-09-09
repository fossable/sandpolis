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

import static com.sandpolis.core.instance.state.STStore.STStore;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractSTObject {

	private List<Object> listeners;

	protected synchronized <T> void fireAttributeEvent(STAttribute<T> attribute, T oldValue, T newValue) {
		if (listeners != null) {
			STStore.pool().submit(() -> {
				for (var listener : listeners) {
					if (listener instanceof STAttribute.EventListener) {
						((STAttribute.EventListener<T>) listener).handle(attribute, oldValue, newValue);
					}
				}
			});
		}
	}

	protected synchronized void fireCollectionEvent(STDocument added, STDocument removed) {
		if (listeners != null) {
			STStore.pool().submit(() -> {
				for (var listener : listeners) {
					if (listener instanceof STCollection.EventListener) {
						((STCollection.EventListener) listener).handle(added, removed);
					}
				}
			});
		}
	}

	public synchronized STCollection.EventListener addListener(STCollection.EventListener listener) {
		if (listeners == null) {
			listeners = new LinkedList<>();
		}
		listeners.add(listener);
		return listener;
	}

	public synchronized <T> STAttribute.EventListener<T> addListener(STAttribute.EventListener<T> listener) {
		if (listeners == null) {
			listeners = new LinkedList<>();
		}
		listeners.add(listener);
		return listener;
	}

	public synchronized void removeListener(Object listener) {
		if (listeners != null) {
			listeners.remove(listener);
		}
		if (listeners.size() == 0) {
			listeners = null;
		}
	}
}
