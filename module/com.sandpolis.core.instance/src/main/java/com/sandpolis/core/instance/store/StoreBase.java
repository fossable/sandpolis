package com.sandpolis.core.instance.store;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.sandpolis.core.instance.event.ParameterizedEvent;
import com.sandpolis.core.instance.storage.database.Database;

/**
 * A Store is designed to provide extremely convenient access to
 * optionally-persistent objects with a static context. Stores cannot be
 * instantiated and may require external initialization before being used.
 */
public abstract class StoreBase<E> {

	/**
	 * A bus that is used to deliver events to the users of the store.
	 */
	private EventBus bus = new EventBus((Throwable exception, SubscriberExceptionContext context) -> {
		exception.printStackTrace();
	});

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

	public StoreBase<E> init(Consumer<E> configurator) {
		bus.post(Event.STORE_INITIALIZED);
		return this;
	}

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

	/**
	 * Post the given {@link Event} to the {@link EventBus}.
	 * 
	 * @param c The event constructor
	 */
	public void post(Supplier<? extends Event> c) {
		bus.post(c.get());
	}

	/**
	 * Post the given {@link ParameterizedEvent} to the {@link EventBus}.
	 * 
	 * @param c         The event constructor
	 * @param parameter The event parameter
	 */
	public <F> void post(Supplier<? extends ParameterizedEvent<F>> c, F parameter) {
		ParameterizedEvent<F> event = c.get();

		try {
			// Set the parameter with reflection
			Field field = event.getClass().getSuperclass().getDeclaredField("object");
			field.setAccessible(true);
			field.set(event, parameter);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		bus.post(event);
	}

	public abstract static class StoreConfig {

		public void ephemeral() {
			throw new UnsupportedOperationException("Store does not support ephemeral providers");
		}

		public void persistent(Database database) {
			throw new UnsupportedOperationException("Store does not support persistent providers");
		}
	}
}
