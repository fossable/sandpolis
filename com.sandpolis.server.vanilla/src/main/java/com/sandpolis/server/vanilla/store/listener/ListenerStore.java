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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.storage.MemoryMapStoreProvider;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.instance.util.ConfigUtil;
import com.sandpolis.core.proto.net.MsgListener.RQ_ChangeListener.ListenerState;
import com.sandpolis.core.proto.pojo.Listener.ListenerConfig;
import com.sandpolis.core.proto.pojo.Listener.ProtoListener;
import com.sandpolis.core.proto.util.Result.ErrorCode;
import com.sandpolis.server.vanilla.store.listener.ListenerStore.ListenerStoreConfig;

/**
 * The {@link ListenerStore} manages network listeners.
 *
 * @author cilki
 * @since 1.0.0
 */
public final class ListenerStore extends MapStore<Long, Listener, ListenerStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(ListenerStore.class);

	public ListenerStore() {
		super(log);
	}

	/**
	 * Start all enabled, unstarted listeners in the store.
	 */
	public void start() {
		try (Stream<Listener> stream = provider.stream()) {
			stream.filter(listener -> !listener.isListening() && listener.isEnabled()).map(listener -> listener.getId())
					.forEach(ListenerStore::start);
		}
	}

	/**
	 * Start the given listener.
	 *
	 * @param id A listener ID
	 */
	public void start(long id) {
		log.debug("Starting listener: {}", id);

		get(id).ifPresent(listener -> listener.start());
	}

	/**
	 * Stop all running listeners in the store.
	 */
	public void stop() {
		try (Stream<Listener> stream = provider.stream()) {
			stream.filter(listener -> listener.isListening()).map(listener -> listener.getId())
					.forEach(ListenerStore::stop);
		}
	}

	/**
	 * Stop the given listener.
	 *
	 * @param id A listener ID
	 */
	public void stop(long id) {
		log.debug("Stopping listener: {}", id);

		get(id).ifPresent(listener -> listener.stop());
	}

	/**
	 * Create a new listener from the given configuration and add it to the store.
	 *
	 * @param config The listener configuration
	 */
	public void add(ListenerConfig.Builder config) {
		add(config.build());
	}

	/**
	 * Create a new listener from the given configuration and add it to the store.
	 *
	 * @param config The listener configuration
	 */
	public void add(ListenerConfig config) {
		Objects.requireNonNull(config);
		checkArgument(ConfigUtil.valid(config) == ErrorCode.OK, "Invalid configuration");
		checkArgument(ConfigUtil.complete(config) == ErrorCode.OK, "Incomplete configuration");

		add(new Listener(config));
	}

	/**
	 * Add a listener to the store.
	 *
	 * @param listener The listener to add
	 */
	public void add(Listener listener) {
		Objects.requireNonNull(listener);
		checkArgument(get(listener.getId()).isEmpty(), "ID conflict");

		log.debug("Adding new listener: {}", listener.getId());
		provider.add(listener);
	}

	/**
	 * Change a listener's configuration or statistics.
	 *
	 * @param id    The ID of the listener to modify
	 * @param delta The changes
	 * @return The outcome of the action
	 */
	public ErrorCode delta(long id, ProtoListener delta) {
		Listener listener = get(id).orElse(null);
		if (listener == null)
			return ErrorCode.UNKNOWN_LISTENER;
		if (listener.isListening())
			return ErrorCode.INVALID_LISTENER_STATE;

		return listener.merge(delta);
	}

	/**
	 * Change a listener's state.
	 *
	 * @param id    A listener ID
	 * @param state The new listener state
	 * @return The outcome of the action
	 */
	public ErrorCode change(long id, ListenerState state) {
		switch (state) {
		case LISTENING:
			start(id);
			break;
		case INACTIVE:
			stop(id);
			break;
		default:
			break;
		}

		return ErrorCode.OK;
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
			provider = new MemoryMapStoreProvider<>(Listener.class, Listener::getId);
		}

		@Override
		public void persistent(Database database) {
			provider = database.getConnection().provider(Listener.class, "id");
		}
	}

	public static final ListenerStore ListenerStore = new ListenerStore();
}
