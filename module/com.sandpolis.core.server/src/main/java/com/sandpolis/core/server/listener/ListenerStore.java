//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.server.listener;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.foundation.ConfigStruct;
import com.sandpolis.core.instance.Listener.ListenerConfig;
import com.sandpolis.core.instance.state.VirtListener;
import com.sandpolis.core.instance.state.vst.VirtCollection;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.STCollectionStore;
import com.sandpolis.core.server.listener.ListenerStore.ListenerStoreConfig;

/**
 * {@link ListenerStore} manages network listeners.
 *
 * @since 1.0.0
 */
public final class ListenerStore extends STCollectionStore<Listener> implements ConfigurableStore<ListenerStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(ListenerStore.class);

	public ListenerStore() {
		super(log);
	}

	/**
	 * Start all enabled, unstarted listeners in the store.
	 */
	public void start() {

		values().stream().filter(listener -> !listener.isActive() && listener.isEnabled()).forEach(listener -> {
			log.info("Starting listener on port: {}", listener.getPort());

			listener.start();
		});
	}

	/**
	 * Stop all running listeners in the store.
	 */
	public void stop() {
		values().stream().filter(listener -> listener.isActive()).forEach(listener -> {
			log.info("Stopping listener on port: {}", listener.getPort());

			listener.stop();
		});
	}

	public Optional<Listener> getByPort(int port) {
		return values().stream().filter(listener -> listener.getPort() == port).findAny();
	}

	@Override
	public void init(Consumer<ListenerStoreConfig> configurator) {
		var config = new ListenerStoreConfig();
		configurator.accept(config);

		collection = config.collection;
	}

	public Listener create(Consumer<VirtListener> configurator) {
		return add(configurator, Listener::new);
	}

	public Listener create(ListenerConfig config) {
		Objects.requireNonNull(config);

		return create(listener -> {
			listener.address().set(config.getAddress());
			listener.port().set(config.getPort());
			listener.name().set(config.getName());
			listener.enabled().set(config.getEnabled());
			listener.active().set(false);
		});
	}

	@ConfigStruct
	public static final class ListenerStoreConfig {

		public VirtCollection<VirtListener> collection;
	}

	/**
	 * The global context {@link ListenerStore}.
	 */
	public static final ListenerStore ListenerStore = new ListenerStore();
}
