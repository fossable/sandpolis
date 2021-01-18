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
import com.sandpolis.core.instance.state.ListenerOid;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.STCollectionStore;
import com.sandpolis.core.server.listener.ListenerStore.ListenerStoreConfig;

/**
 * {@link ListenerStore} manages network listeners.
 *
 * @since 1.0.0
 */
public final class ListenerStore extends STCollectionStore<Listener> implements ConfigurableStore<ListenerStoreConfig> {

	@ConfigStruct
	public static final class ListenerStoreConfig {

		public STDocument collection;
	}

	/**
	 * The global context {@link ListenerStore}.
	 */
	public static final ListenerStore ListenerStore = new ListenerStore();

	private static final Logger log = LoggerFactory.getLogger(ListenerStore.class);

	public ListenerStore() {
		super(log, Listener::new);
	}

	public Listener create(ListenerConfig config) {
		Objects.requireNonNull(config);

		return create(listener -> {
			listener.set(ListenerOid.ADDRESS, config.getAddress());
			listener.set(ListenerOid.PORT, config.getPort());
			listener.set(ListenerOid.NAME, config.getName());
			listener.set(ListenerOid.ENABLED, config.getEnabled());
			listener.set(ListenerOid.ACTIVE, false);
		});
	}

	public Optional<Listener> getByPort(int port) {
		return values().stream().filter(listener -> listener.get(ListenerOid.PORT) == port).findAny();
	}

	@Override
	public void init(Consumer<ListenerStoreConfig> configurator) {
		var config = new ListenerStoreConfig();
		configurator.accept(config);

		setDocument(config.collection);
	}

	/**
	 * Start all enabled, unstarted listeners in the store.
	 */
	public void start() {

		values().stream().filter(listener -> !listener.get(ListenerOid.ACTIVE) && listener.get(ListenerOid.ENABLED))
				.forEach(listener -> {
					log.info("Starting listener on port: {}", listener.get(ListenerOid.PORT));

					listener.start();
				});
	}

	/**
	 * Stop all running listeners in the store.
	 */
	public void stop() {
		values().stream().filter(listener -> listener.get(ListenerOid.ACTIVE)).forEach(listener -> {
			log.info("Stopping listener on port: {}", listener.get(ListenerOid.PORT));

			listener.stop();
		});
	}
}
