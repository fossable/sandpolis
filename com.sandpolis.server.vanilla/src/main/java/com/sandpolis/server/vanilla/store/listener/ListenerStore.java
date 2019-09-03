/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.server.vanilla.store.listener;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.storage.StoreProvider;
import com.sandpolis.core.instance.store.StoreBase;
import com.sandpolis.core.proto.net.MCListener.RQ_ChangeListener.ListenerState;
import com.sandpolis.core.proto.pojo.Listener.ListenerConfig;
import com.sandpolis.core.proto.pojo.Listener.ProtoListener;
import com.sandpolis.core.proto.util.Result.ErrorCode;
import com.sandpolis.core.util.ValidationUtil;
import com.sandpolis.server.vanilla.store.listener.ListenerStore.ListenerStoreConfig;

/**
 * The {@link ListenerStore} manages network listeners.
 * 
 * @author cilki
 * @since 1.0.0
 */
public final class ListenerStore extends StoreBase<ListenerStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(ListenerStore.class);

	private static StoreProvider<Listener> provider;

	/**
	 * Start all enabled, unstarted listeners in the store.
	 */
	public static void start() {
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
	public static void start(long id) {
		log.debug("Starting listener: {}", id);

		get(id).ifPresent(listener -> listener.start());
	}

	/**
	 * Stop all running listeners in the store.
	 */
	public static void stop() {
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
	public static void stop(long id) {
		log.debug("Stopping listener: {}", id);

		get(id).ifPresent(listener -> listener.stop());
	}

	/**
	 * Get a listener in the store.
	 * 
	 * @param id A listener ID
	 * @return The associated listener
	 */
	public static Optional<Listener> get(long id) {
		return provider.get("id", id);
	}

	/**
	 * Create a new listener from the given configuration and add it to the store.
	 * 
	 * @param config The listener configuration
	 */
	public static void add(ListenerConfig.Builder config) {
		add(config.build());
	}

	/**
	 * Create a new listener from the given configuration and add it to the store.
	 * 
	 * @param config The listener configuration
	 */
	public static void add(ListenerConfig config) {
		Objects.requireNonNull(config);
		checkArgument(ValidationUtil.Config.valid(config) == ErrorCode.OK, "Invalid configuration");
		checkArgument(ValidationUtil.Config.complete(config) == ErrorCode.OK, "Incomplete configuration");

		add(new Listener(config));
	}

	/**
	 * Add a listener to the store.
	 * 
	 * @param listener The listener to add
	 */
	public static void add(Listener listener) {
		Objects.requireNonNull(listener);
		checkArgument(get(listener.getId()).isEmpty(), "ID conflict");

		log.debug("Adding new listener: {}", listener.getId());
		provider.add(listener);
	}

	/**
	 * Remove a listener from the store.
	 * 
	 * @param id A listener ID
	 */
	public static void remove(long id) {
		log.debug("Deleting listener {}", id);

		get(id).ifPresent(provider::remove);
	}

	/**
	 * Change a listener's configuration or statistics.
	 * 
	 * @param id    The ID of the listener to modify
	 * @param delta The changes
	 * @return The outcome of the action
	 */
	public static ErrorCode delta(long id, ProtoListener delta) {
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
	public static ErrorCode change(long id, ListenerState state) {
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

	private ListenerStore() {
	}

	@Override
	public void init(Consumer<ListenerStoreConfig> o) {
		// TODO Auto-generated method stub

	}

	public static final class ListenerStoreConfig {

	}

	public static final ListenerStore ListenerStore = new ListenerStore();
}
