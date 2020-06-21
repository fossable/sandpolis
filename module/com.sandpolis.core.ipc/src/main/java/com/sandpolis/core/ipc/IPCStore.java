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
package com.sandpolis.core.ipc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.store.StoreBase;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.core.instance.store.pref.PrefStore;
import com.sandpolis.core.ipc.IPCStore.IPCStoreConfig;
import com.sandpolis.core.ipc.Message.MSG;
import com.sandpolis.core.ipc.Message.MSG.PayloadCase;
import com.sandpolis.core.ipc.Metadata.RS_Metadata;
import com.sandpolis.core.util.Platform.Instance;
import com.sandpolis.core.util.Platform.InstanceFlavor;

/**
 * This store manages interprocess connections.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class IPCStore extends StoreBase<IPCStoreConfig> {

	public static final Logger log = LoggerFactory.getLogger(IPCStore.class);

	public IPCStore() {
		super(log);
	}

	/**
	 * An IPC message handler.
	 */
	public static interface Handler {
		public void handle(MSG message, OutputStream out) throws IOException;
	}

	/**
	 * A list of open incoming connections spawned from the listener.
	 */
	private List<Receptor> receptors;

	/**
	 * A list of open outgoing connections.
	 */
	private List<Connector> connectors;

	/**
	 * A list of running listeners.
	 */
	private List<Listener> listeners;

	/**
	 * A list of registered message handlers.
	 */
	private Map<PayloadCase, Handler> handlers;

	/**
	 * Register a new message handler for the given type.
	 *
	 * @param type    The message type
	 * @param handler The message handler
	 */
	public void register(PayloadCase type, Handler handler) {
		handlers.put(type, handler);
	}

	/**
	 * Get an unmodifiable view of the {@link Receptor} list.
	 *
	 * @return The store's receptor list
	 */
	public List<Receptor> getReceptors() {
		return Collections.unmodifiableList(receptors);
	}

	List<Receptor> getMutableReceptors() {
		return receptors;
	}

	/**
	 * Get an unmodifiable view of the {@link Connector} list.
	 *
	 * @return The store's connector list
	 */
	public List<Connector> getConnectors() {
		return Collections.unmodifiableList(connectors);
	}

	List<Connector> getMutableConnectors() {
		return connectors;
	}

	/**
	 * Get an unmodifiable view of the {@link Listener} list.
	 *
	 * @return The store's listener list
	 */
	public List<Listener> getListeners() {
		return Collections.unmodifiableList(listeners);
	}

	List<Listener> getMutableListeners() {
		return listeners;
	}

	/**
	 * Get an unmodifiable view of the {@link Handler} list.
	 *
	 * @return The store's message handlers
	 */
	public Map<PayloadCase, Handler> getHandlers() {
		return Collections.unmodifiableMap(handlers);
	}

	/**
	 * Attempt to get an instance's metadata over IPC.
	 *
	 * @param instance The instance type to target
	 * @param flavor   The instance subtype
	 * @return The received metadata object or {@code null} if an error occurred.
	 */
	public Optional<RS_Metadata> queryInstance(Instance instance, InstanceFlavor flavor) {
		log.debug("Performing IPC query for {}:{} instances", instance, flavor);

		try (Connector connector = connect(instance, flavor)) {
			return connector.rq_metadata();
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	/**
	 * Start a new listener for the given instance type.
	 *
	 * @param instance The instance type
	 * @param flavor   The instance subtype
	 * @throws IOException
	 */
	public void listen(Instance instance, InstanceFlavor flavor) throws IOException {
		Objects.requireNonNull(instance);
		Objects.requireNonNull(flavor);

		Listener listener = new Listener();

		Preferences preferences = PrefStore.getPreferences(instance, flavor);
		preferences.putInt("ipc.port", listener.getPort());
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			listener.close();
			throw new IOException(e);
		}

		listener.start(Executors.newSingleThreadExecutor(), Executors.newSingleThreadExecutor());
		listeners.add(listener);

		log.debug("Opened IPC listener on port: {}", listener.getPort());
	}

	/**
	 * Make a connection to the given instance type.
	 *
	 * @param instance The instance type
	 * @param flavor   The instance subtype
	 * @return The established connection
	 * @throws IOException
	 */
	public Connector connect(Instance instance, InstanceFlavor flavor) throws IOException {
		Objects.requireNonNull(instance);
		Objects.requireNonNull(flavor);

		// Find the port
		int port = getPort(instance, flavor);
		if (port == 0)
			throw new IOException("Failed to find IPC port");

		log.debug("Attempting IPC connection on port: {}", port);
		return new Connector(port);
	}

	/**
	 * Get the IPC listening port for the specified {@link Instance}.
	 *
	 * @param instance The instance type
	 * @param flavor   The instance subtype
	 * @return The IPC port or 0 for not found
	 */
	public int getPort(Instance instance, InstanceFlavor flavor) {
		return PrefStore.getPreferences(instance, flavor).getInt("ipc.port", 0);
	}

	@Override
	public IPCStore init(Consumer<IPCStoreConfig> configurator) {
		var config = new IPCStoreConfig();
		configurator.accept(config);

		register(PayloadCase.RQ_METADATA, (MSG message, OutputStream out) -> {
			MSG.newBuilder()
					.setRsMetadata(RS_Metadata.newBuilder().setInstance(Core.INSTANCE.name())
							.setVersion(Core.SO_BUILD.getVersion()).setPid(ProcessHandle.current().pid()))
					.build().writeDelimitedTo(out);
		});

		return (IPCStore) super.init(null);
	}

	public final class IPCStoreConfig extends StoreConfig {

		@Override
		public void ephemeral() {
			receptors = Collections.synchronizedList(new LinkedList<>());
			connectors = Collections.synchronizedList(new LinkedList<>());
			listeners = Collections.synchronizedList(new LinkedList<>());
			handlers = Collections.synchronizedMap(new HashMap<>());
		}
	}

	public static final IPCStore IPCStore = new IPCStore();
}
