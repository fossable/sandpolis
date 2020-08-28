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

import java.util.LinkedList;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.state.StateEventStore.StateEventStoreConfig;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.StoreBase;
import com.sandpolis.core.instance.store.StoreConfig;

public final class StateEventStore extends StoreBase implements ConfigurableStore<StateEventStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(StateEventStore.class);

	public StateEventStore() {
		super(log);
	}

	public static class StateEvent {
		public Oid<?> oid;
		public Object oldValue;
		public Object newValue;

		public StateEvent(Oid<?> oid, Object oldValue, Object newValue) {
			this.oid = oid;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}
	}

	private TreeMap<Oid<?>, LinkedList<Consumer<StateEvent>>> listenerTree;

	private ExecutorService service;

	/**
	 * The events waiting to be processed.
	 */
	private BlockingQueue<StateEvent> eventQueue;

	private void processEvents() {
		StateEvent event;
		try {
			while ((event = eventQueue.take()) != null) {
				for (int i = 1; i < event.oid.size(); i++) {
					var listeners = listenerTree.get(event.oid.head(i));
					if (listeners != null) {
						for (var listener : listeners) {
							listener.accept(event);
						}
					}
				}
			}
		} catch (InterruptedException e) {
			// Exit
		}
	}

	public <T> boolean fire(Oid<T> oid, T oldValue, T newValue) {
		return eventQueue.offer(new StateEvent(oid, oldValue, newValue));
	}

	public <T> void bind(Oid<T> oid, Consumer<StateEvent> listener) {
		var listeners = listenerTree.get(oid);
		if (listeners == null) {
			listeners = new LinkedList<>();
			listenerTree.put(oid, listeners);
		}
		listeners.add(listener);
	}

	@Override
	public void init(Consumer<StateEventStoreConfig> configurator) {
		var config = new StateEventStoreConfig();
		configurator.accept(config);

		listenerTree = new TreeMap<>();
		eventQueue = new ArrayBlockingQueue<>(config.queueSize);

		// Launch executors
		service = Executors.newSingleThreadExecutor();
		service.execute(this::processEvents);
	}

	@Override
	public void close() throws Exception {
		service.shutdown();
	}

	public final class StateEventStoreConfig extends StoreConfig {
		public int queueSize;
		public int concurrency;
	}

	public static final StateEventStore StateEventStore = new StateEventStore();
}
