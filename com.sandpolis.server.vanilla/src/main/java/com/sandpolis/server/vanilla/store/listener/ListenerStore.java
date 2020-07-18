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
package com.sandpolis.server.vanilla.store.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Listener.ListenerConfig;
import com.sandpolis.core.instance.store.MemoryMapStoreProvider;
import com.sandpolis.core.instance.database.Database;
import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.server.vanilla.store.listener.ListenerStore.ListenerStoreConfig;

/**
 * The {@link ListenerStore} manages network listeners.
 *
 * @author cilki
 * @since 1.0.0
 */
public final class ListenerStore extends MapStore<Listener, ListenerStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(ListenerStore.class);

	public void add(ListenerConfig config) {
		Objects.requireNonNull(config);

		add(new Listener(document, config));
	}

	/**
	 * Start all enabled, unstarted listeners in the store.
	 */
	public void start() {
		stream().filter(listener -> !listener.isActive() && listener.isEnabled()).forEach(listener -> {
			log.info("Starting listener on port: {}", listener.getPort());

			listener.start();
		});
	}

	/**
	 * Stop all running listeners in the store.
	 */
	public void stop() {
		stream().filter(listener -> listener.isActive()).forEach(listener -> {
			log.info("Stopping listener on port: {}", listener.getPort());

			listener.stop();
		});
	}

	public Optional<Listener> getByPort(int port) {
		return stream().filter(listener -> listener.getPort() == port).findAny();
	}

	@Override
	public ListenerStore init(Consumer<ListenerStoreConfig> configurator) {
		var config = new ListenerStoreConfig();
		configurator.accept(config);

		config.defaults.forEach(this::add);

		return (ListenerStore) super.init(null);
	}

	public final class ListenerStoreConfig extends StoreConfig {

		public final List<ListenerConfig> defaults = new ArrayList<>();

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(Listener.class);
		}

		@Override
		public void persistent(Database database) {
			provider = database.getConnection().provider(Listener.class);
		}
	}

	public ListenerStore() {
		super(log);
	}

	/**
	 * The global context {@link ListenerStore}.
	 */
	public static final ListenerStore ListenerStore = new ListenerStore();
}
