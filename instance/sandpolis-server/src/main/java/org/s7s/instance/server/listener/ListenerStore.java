//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.listener;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.instance.Listener.ListenerConfig;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ServerOid.ListenerOid;
import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.store.ConfigurableStore;
import org.s7s.core.instance.store.STCollectionStore;
import org.s7s.core.server.listener.ListenerStore.ListenerStoreConfig;

/**
 * {@link ListenerStore} manages network listeners.
 *
 * @since 1.0.0
 */
public final class ListenerStore extends STCollectionStore<Listener> implements ConfigurableStore<ListenerStoreConfig> {

	public static final class ListenerStoreConfig {

		public STDocument collection;

		private ListenerStoreConfig(Consumer<ListenerStoreConfig> configurator) {
			configurator.accept(this);
		}
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
		return values().stream().filter(listener -> listener.get(ListenerOid.PORT).asInt() == port).findAny();
	}

	@Override
	public void init(Consumer<ListenerStoreConfig> configurator) {
		var config = new ListenerStoreConfig(configurator);

		setDocument(config.collection);
	}

	/**
	 * Start all enabled, unstarted listeners in the store.
	 */
	public void start() {

		values().stream().filter(listener -> !listener.get(ListenerOid.ACTIVE).asBoolean()
				&& listener.get(ListenerOid.ENABLED).asBoolean()).forEach(listener -> {
					log.info("Starting listener on port: {}", listener.get(ListenerOid.PORT).asInt());

					listener.start();
				});
	}

	/**
	 * Stop all running listeners in the store.
	 */
	public void stop() {
		values().stream().filter(listener -> listener.get(ListenerOid.ACTIVE).asBoolean()).forEach(listener -> {
			log.info("Stopping listener on port: {}", listener.get(ListenerOid.PORT).asInt());

			listener.stop();
		});
	}
}
