package com.sandpolis.core.instance.store;

import java.util.function.Consumer;

import com.google.common.eventbus.EventBus;

/**
 * A Store is designed to provide extremely convenient access to
 * optionally-persistent objects with a static context. Stores cannot be
 * instantiated and may require external initialization before being used.
 */
public abstract class StoreBase<E> {

	/**
	 * A bus that is used to deliver events to the users of the store.
	 */
	private EventBus bus = new EventBus();

	public void register(Object object) {
		bus.register(object);
	}

	public void unregister(Object object) {
		bus.unregister(object);
	}

	/**
	 * Uninitialize and release the resources in the store.
	 * 
	 * @throws Exception
	 */
	public void close() throws Exception {
		bus.post(Event.STORE_CLOSE);
	}

	public abstract void init(Consumer<E> o);

	public static enum Event {
		/**
		 * Indicates that the store has just been fully initialized.
		 */
		STORE_INITIALIZED,

		/**
		 * Indicates that the store is about to close.
		 */
		STORE_CLOSE;
	}
}
