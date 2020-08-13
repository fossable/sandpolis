package com.sandpolis.core.instance.store;

import java.util.function.Consumer;

/**
 * {@link ConfigurableStore} is a store that requires initialization by
 * consumers before it can be used. Initialization is idempotent and may happen
 * more than once.
 *
 * @param <E> The configuration type
 */
public interface ConfigurableStore<E extends StoreConfig> {

	/**
	 * Initialize the store with the given configurator.
	 *
	 * @param configurator The initialization block
	 */
	public void init(Consumer<E> configurator);
}
