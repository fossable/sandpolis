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
package com.sandpolis.server.store.listener;

import static com.sandpolis.core.util.ProtoUtil.begin;
import static com.sandpolis.core.util.ProtoUtil.complete;
import static com.sandpolis.core.util.ProtoUtil.failure;
import static com.sandpolis.core.util.ProtoUtil.success;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.OutcomeSet;
import com.sandpolis.core.instance.Store;
import com.sandpolis.core.instance.storage.StoreProvider;
import com.sandpolis.core.instance.storage.StoreProviderFactory;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.proto.net.MCListener.RQ_ChangeListener.ListenerState;
import com.sandpolis.core.proto.util.Listener.ListenerConfig;
import com.sandpolis.core.proto.util.Result.Outcome;

/**
 * The {@link ListenerStore} manages network listeners.
 * 
 * @author cilki
 * @since 1.0.0
 */
public final class ListenerStore extends Store {
	private ListenerStore() {
	}

	private static final Logger log = LoggerFactory.getLogger(ListenerStore.class);

	private static StoreProvider<Listener> provider;

	public static void init(StoreProvider<Listener> provider) {
		if (provider == null)
			throw new IllegalArgumentException();

		ListenerStore.provider = provider;
	}

	public static void load(Database main) {
		if (main == null)
			throw new IllegalArgumentException();

		init(StoreProviderFactory.database(Listener.class, main));
	}

	/**
	 * Start all enabled, unstarted listeners in the store.
	 * 
	 * @return An {@code OutcomeSet} representing the total outcome
	 */
	public static OutcomeSet start() {
		OutcomeSet outcomes = new OutcomeSet();
		provider.stream().filter(listener -> !listener.isListening() && listener.isEnabled()).forEach(listener -> {
			Outcome.Builder outcome = begin();

			log.debug("Starting listener: {}", listener.getId());
			outcome.setResult(listener.start());
			outcomes.add(complete(outcome));
		});
		return outcomes;
	}

	/**
	 * Start the given listener.
	 * 
	 * @param id A listener ID
	 * @return The outcome of the action
	 */
	public static Outcome start(long id) {
		Outcome.Builder outcome = begin();
		Listener listener = get(id);
		if (listener == null)
			return failure(outcome, "The listener was not found");
		if (!listener.isEnabled())
			return failure(outcome, "The listener is not enabled");
		if (listener.isListening())
			return failure(outcome, "The listener is already running");

		log.debug("Starting listener: {}", listener.getId());
		outcome.setResult(listener.start());
		return complete(outcome);
	}

	/**
	 * Stop all running listeners in the store.
	 * 
	 * @return An {@code OutcomeSet} representing the total outcome
	 */
	public static OutcomeSet stop() {
		OutcomeSet outcomes = new OutcomeSet();
		provider.stream().filter(listener -> listener.isListening()).forEach(listener -> {
			Outcome.Builder outcome = begin();

			log.debug("Stopping listener: {}", listener.getId());
			listener.stop();
			outcomes.add(complete(outcome));
		});
		return outcomes;
	}

	/**
	 * Stop the given listener.
	 * 
	 * @param id A listener ID
	 * @return The outcome of the action
	 */
	public static Outcome stop(long id) {
		Outcome.Builder outcome = begin();
		Listener listener = get(id);
		if (listener == null)
			return failure(outcome, "The listener was not found");
		if (!listener.isListening())
			return failure(outcome, "The listener is not running");

		log.debug("Stopping listener: {}", listener.getId());
		listener.stop();
		return success(outcome);
	}

	/**
	 * Get a listener in the store.
	 * 
	 * @param id A listener ID
	 * @return The associated listener
	 */
	public static Listener get(long id) {
		return provider.get("id", id);
	}

	/**
	 * Create a new listener from the given configuration and add it to the store.
	 * 
	 * @param config
	 * @return
	 */
	public static Outcome add(ListenerConfig config) {
		try {
			return add(new Listener(config));
		} catch (IllegalArgumentException e) {
			return Outcome.newBuilder().setResult(false).build();
		}
	}

	/**
	 * Add a listener to the store.
	 * 
	 * @param listener The listener to add
	 * @return The outcome of the action
	 */
	public static Outcome add(Listener listener) {
		Outcome.Builder outcome = begin();
		if (get(listener.getId()) != null)
			return failure(outcome, "Listener ID is already taken");

		provider.add(listener);
		return success(outcome);
	}

	/**
	 * Remove a listener from the store.
	 * 
	 * @param id A listener ID
	 * @return The outcome of the action
	 */
	public static Outcome remove(long id) {
		Outcome.Builder outcome = begin();
		Listener listener = get(id);
		if (listener == null)
			return failure(outcome, "Listener not found");
		if (listener.isListening())
			return failure(outcome, "Listener is active");

		provider.remove(listener);
		return success(outcome);
	}

	/**
	 * Edit a listener's configuration.
	 * 
	 * @param id      A listener ID
	 * @param changes A list of changed field numbers
	 * @param config  The configuration changes
	 * @return The outcome of the action
	 */
	public static Outcome edit(long id, List<Integer> changed, ListenerConfig config) {
		Outcome.Builder outcome = begin();
		Listener listener = get(id);
		if (listener == null)
			return failure(outcome, "Listener not found");
		if (listener.isListening())
			return failure(outcome, "Listener is active");

		// TODO error handling
		if (changed.contains(ListenerConfig.NAME_FIELD_NUMBER))
			listener.setName(config.getName());
		if (changed.contains(ListenerConfig.PORT_FIELD_NUMBER))
			listener.setPort(config.getPort());
		if (changed.contains(ListenerConfig.ADDRESS_FIELD_NUMBER))
			listener.setAddress(config.getAddress());
		// TODO owner
		if (changed.contains(ListenerConfig.UPNP_FIELD_NUMBER))
			listener.setUpnp(config.getUpnp());
		if (changed.contains(ListenerConfig.CLIENT_ACCEPTOR_FIELD_NUMBER))
			listener.setClientAcceptor(config.getClientAcceptor());
		if (changed.contains(ListenerConfig.VIEWER_ACCEPTOR_FIELD_NUMBER))
			listener.setViewerAcceptor(config.getViewerAcceptor());
		if (changed.contains(ListenerConfig.ENABLED_FIELD_NUMBER))
			listener.setEnabled(config.getEnabled());
		if (changed.contains(ListenerConfig.CERT_FIELD_NUMBER))
			listener.setCertificate(config.getCert().toByteArray());
		if (changed.contains(ListenerConfig.KEY_FIELD_NUMBER))
			listener.setPrivateKey(config.getKey().toByteArray());

		return success(outcome);
	}

	/**
	 * Change a listener's state.
	 * 
	 * @param id    A listener ID
	 * @param state The new listener state
	 * @return The outcome of the action
	 */
	public static Outcome change(long id, ListenerState state) {
		switch (state) {
		case LISTENING:
			return start(id);
		case INACTIVE:
			return stop(id);
		default:
			return Outcome.newBuilder().setResult(false).build();
		}
	}
}
