//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.store;

import static org.s7s.core.instance.thread.ThreadStore.ThreadStore;

import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;

/**
 * <p>
 * At a high level, a Store provides an interface to operations on an entity (or
 * on a collection of entities) from a convenient application context.
 *
 * <p>
 * This root class defines a highly general structure applicable to all Stores
 * and handles operations like lifecycle management and event propagation.
 *
 * @see STCollectionStore
 */
public abstract class StoreBase {

	private Logger log;

	/**
	 * A bus that is used to deliver events to the users of the store.
	 */
	private final EventBus bus = new EventBus((Throwable exception, SubscriberExceptionContext context) -> {
		log.error("Store event handler exception", exception);
	});

	protected StoreBase(Logger log) {
		this.log = log;
	}

	/**
	 * Uninitialize and release the resources in the store. By default, this method
	 * does nothing.
	 *
	 * @throws Exception
	 */
	public void close() throws Exception {
	}

	/**
	 * Broadcast the given event to the store's bus. This method blocks until every
	 * event handler completes.
	 *
	 * @param event The event to post
	 */
	public final void post(Object event) {

		// Logging here seems to be a problem?
//		if (log.isDebugEnabled())
//			log.debug("Event fired: {}", event);

		bus.post(event);
	}

	/**
	 * Broadcast the given event asynchronously to the store's bus.
	 *
	 * @param event The event to post
	 */
	public final void postAsync(Object event) {
		ThreadStore.get("store.event_bus").submit(() -> {
			post(event);
		});
	}

	/**
	 * Add the given subscriber from the store bus.
	 *
	 * @param object The subscriber to add
	 */
	public final void register(Object object) {
		try {
			bus.unregister(object);
		} catch (IllegalArgumentException e) {
			assert true;
		}
		bus.register(object);
	}

	/**
	 * Remove the given subscriber from the store bus.
	 *
	 * @param object The subscriber to remove
	 */
	public final void unregister(Object object) {
		bus.unregister(object);
	}
}
