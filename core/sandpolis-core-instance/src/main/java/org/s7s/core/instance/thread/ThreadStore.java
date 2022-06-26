//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.instance.store.ConfigurableStore;
import org.s7s.core.instance.store.StoreBase;
import org.s7s.core.instance.thread.ThreadStore.ThreadStoreConfig;

/**
 * {@link ThreadStore} manages all of the application's {@link ExecutorService}
 * objects.
 *
 * @since 5.0.0
 */
public final class ThreadStore extends StoreBase implements ConfigurableStore<ThreadStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(ThreadStore.class);

	public ThreadStore() {
		super(log);
	}

	private Map<String, ExecutorService> container;

	/**
	 * Get the {@link ExecutorService} corresponding to the given identifier.
	 *
	 * @param id The identifier
	 * @return A {@link ExecutorService} or {@code null} if the service does not
	 *         exist
	 */
	@SuppressWarnings("unchecked")
	public <E extends ExecutorService> E get(String id) {
		return (E) container.get(Objects.requireNonNull(id));
	}

	@Override
	public void close() throws Exception {
		container.entrySet().forEach(entry -> {
			log.debug("Closing thread pool: " + entry.getKey());
			entry.getValue().shutdownNow();
		});
		container = null;
	}

	@Override
	public void init(Consumer<ThreadStoreConfig> configurator) {
		var config = new ThreadStoreConfig(configurator);

		container = new HashMap<>();
		container.putAll(config.defaults);
	}

	public static final class ThreadStoreConfig {

		public final Map<String, ExecutorService> defaults = new HashMap<>();

		private ThreadStoreConfig(Consumer<ThreadStoreConfig> configurator) {
			configurator.accept(this);
		}
	}

	public static final ThreadStore ThreadStore = new ThreadStore();
}
