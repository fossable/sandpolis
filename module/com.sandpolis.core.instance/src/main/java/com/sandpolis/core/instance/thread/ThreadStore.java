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
package com.sandpolis.core.instance.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.store.StoreBase;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.core.instance.store.StoreMetadata;
import com.sandpolis.core.instance.thread.ThreadStore.ThreadStoreConfig;

/**
 * The {@link ThreadStore} manages all of the application's
 * {@link ExecutorService} objects.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class ThreadStore extends StoreBase<ThreadStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(ThreadStore.class);

	public ThreadStore() {
		super(log);
	}

	private Map<String, ExecutorService> provider;

	/**
	 * Get the {@link ExecutorService} corresponding to the given identifier.
	 *
	 * @param id The identifier
	 * @return A {@link ExecutorService} or {@code null} if the service does not
	 *         exist
	 */
	@SuppressWarnings("unchecked")
	public <E extends ExecutorService> E get(String id) {
		return (E) provider.get(Objects.requireNonNull(id));
	}

	@Override
	public void close() throws Exception {
		log.debug("Closing {} active thread pools", provider.size());
		provider.values().forEach(service -> service.shutdownNow());
		provider = null;
	}

	@Override
	public void init(Consumer<ThreadStoreConfig> configurator) {
		var config = new ThreadStoreConfig();
		configurator.accept(config);

		provider.putAll(config.defaults);
	}

	public final class ThreadStoreConfig extends StoreConfig {

		public final Map<String, ExecutorService> defaults = new HashMap<>();

		@Override
		public void ephemeral() {
			provider = new HashMap<>();
		}

	}

	public static final ThreadStore ThreadStore = new ThreadStore();

	@Override
	public StoreMetadata getMetadata() {
		// TODO Auto-generated method stub
		return null;
	}
}
