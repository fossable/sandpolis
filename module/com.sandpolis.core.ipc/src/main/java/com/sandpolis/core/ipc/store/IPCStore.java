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
package com.sandpolis.core.ipc.store;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Store;
import com.sandpolis.core.instance.Store.AutoInitializer;
import com.sandpolis.core.ipc.Connector;
import com.sandpolis.core.ipc.Listener;
import com.sandpolis.core.ipc.Receptor;
import com.sandpolis.core.ipc.MCMetadata.RS_Metadata;
import com.sandpolis.core.proto.util.Platform.Instance;

/**
 * This store manages interprocess connections.
 * 
 * @author cilki
 * @since 5.0.0
 */
@AutoInitializer
public final class IPCStore extends Store {
	private IPCStore() {
	}

	public static final Logger log = LoggerFactory.getLogger(IPCStore.class);

	/**
	 * Default IPC port.
	 */
	public static final int SERVER_PORT = 10111;

	/**
	 * Default IPC port.
	 */
	public static final int VIEWER_PORT = 10112;

	/**
	 * Default IPC port.
	 */
	public static final int CLIENT_PORT = 10113;

	/**
	 * The IPC listener for the entire instance.
	 */
	private static Listener listener;

	/**
	 * A list of open incoming connections spawned from the listener.
	 */
	private static List<Receptor> receptors = new LinkedList<>();

	/**
	 * A list of open outgoing connections.
	 */
	private static List<Connector> connectors = new LinkedList<>();

	/**
	 * Add a {@link Connector} to the store.
	 * 
	 * @param connector The {@link Connector} to add
	 */
	public static void add(Connector connector) {
		connectors.add(connector);
	}

	/**
	 * Add a {@link Receptor} to the store.
	 * 
	 * @param receptor The {@link Receptor} to add
	 */
	public static void add(Receptor receptor) {
		receptors.add(receptor);
	}

	/**
	 * Remove a {@link Connector} from the store.
	 * 
	 * @param connector The {@link Connector} to remove
	 */
	public static void remove(Connector connector) {
		connectors.remove(connector);
	}

	/**
	 * Remove a {@code Receptor} from the store.
	 * 
	 * @param receptor The {@code Receptor} to remove
	 */
	public static void remove(Receptor receptor) {
		receptors.remove(receptor);
	}

	/**
	 * Remove the {@link Listener} from the store.
	 * 
	 * @param listener The {@link Listener} to remove which should be the same as
	 *                 the single listener already in the store.
	 */
	public static void remove(Listener listener) {
		if (listener == null)
			throw new IllegalArgumentException();
		if (IPCStore.listener != listener)
			throw new IllegalStateException("More than one listener detected");

		IPCStore.listener.interrupt();
		IPCStore.listener = null;
	}

	/**
	 * Attempt to get an instance's metadata over IPC.
	 * 
	 * @param instance The instance type to target
	 * @return The received metadata object or {@code null} if an error occurred.
	 */
	public static RS_Metadata queryInstance(Instance instance) {
		log.debug("Performing IPC query for {} instances on port: {}", instance, getPort(instance));

		try (Connector connector = connect(instance)) {
			return connector.rq_metadata();
		} catch (IOException e) {
			return null;
		} catch (Exception e) {
			// Failed to close connector
			return null;
		}
	}

	/**
	 * Start a new listener for the given instance type.
	 * 
	 * @param instance The instance type
	 * @throws IOException
	 */
	public static void listen(Instance instance) throws IOException {
		if (instance == null)
			throw new IllegalArgumentException();
		if (listener != null)
			throw new IllegalStateException("Only one IPC listener can be registered at a time");

		listener = new Listener(getPort(instance));
	}

	/**
	 * Make a connection to the given instance type on the default port.
	 * 
	 * @param instance The instance to connect
	 * @return The established connection
	 * @throws IOException
	 */
	public static Connector connect(Instance instance) throws IOException {
		if (instance == null)
			throw new IllegalArgumentException();

		return new Connector(getPort(instance));
	}

	/**
	 * Get the IPC listening port for the specified {@link Instance}.
	 * 
	 * @param instance The instance.
	 * @return The IPC port
	 */
	public static int getPort(Instance instance) {
		switch (instance) {
		case CLIENT:
			return CLIENT_PORT;
		case SERVER:
			return SERVER_PORT;
		case VIEWER:
			return VIEWER_PORT;
		default:
			return 0;
		}
	}

}
