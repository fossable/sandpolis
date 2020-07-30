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
import com.sandpolis.core.instance.data.Document;
import com.sandpolis.core.instance.store.CollectionStore;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.core.instance.store.provider.MemoryMapStoreProvider;
import com.sandpolis.core.instance.store.provider.StoreProviderFactory;
import com.sandpolis.server.vanilla.store.listener.ListenerStore.ListenerStoreConfig;

/**
 * The {@link ListenerStore} manages network listeners.
 *
 * @author cilki
 * @since 1.0.0
 */
public final class ListenerStore extends CollectionStore<Listener, ListenerStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(ListenerStore.class);

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
	public void init(Consumer<ListenerStoreConfig> configurator) {
		var config = new ListenerStoreConfig();
		configurator.accept(config);

		config.defaults.forEach(this::create);

		provider.initialize();
	}

	@Override
	public Listener create(Consumer<Listener> configurator) {
		var listener = new Listener(new Document(provider.getCollection()));
		configurator.accept(listener);
		provider.add(listener);
		return listener;
	}

	public Listener create(ListenerConfig config) {
		Objects.requireNonNull(config);

		return create(listener -> {
			listener.address().set(config.getAddress());
			listener.port().set(config.getPort());
			listener.name().set(config.getName());
			listener.enabled().set(config.getEnabled());
		});
	}

	public final class ListenerStoreConfig extends StoreConfig {

		public final List<ListenerConfig> defaults = new ArrayList<>();

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(Listener.class, Listener::tag);
		}

		@Override
		public void persistent(StoreProviderFactory factory) {
			provider = factory.supply(Listener.class, Listener::new);
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
