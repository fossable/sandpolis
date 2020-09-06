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

public class DefaultObject<A extends STObject<?>, T> {

	interface STEventListener<A extends STObject<?>, T> {
		public void handle(A entity, T oldValue, T newValue);
	}

	private List<STEventListener<A, T>> listeners;

	protected synchronized void fire(A entity, T oldValue, T newValue) {
		if (listeners != null) {
			STStore.pool().submit(() -> {
				for (var listener : listeners) {
					listener.handle(entity, oldValue, newValue);
				}
			});
		}
	}

	public synchronized void bind(STEventListener<A, T> listener) {
		if (listeners == null) {
			listeners = new LinkedList<>();
		}
		listeners.add(listener);
	}

	public synchronized void unbind(STEventListener<A, T> listener) {
		if (listeners != null) {
			listeners.remove(listener);
		}
		if (listeners.size() == 0) {
			listeners = null;
		}
	}
}
