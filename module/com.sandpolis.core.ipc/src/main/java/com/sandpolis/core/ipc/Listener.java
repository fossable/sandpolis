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

import static com.sandpolis.core.ipc.IPCStore.IPCStore;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.RateLimiter;

/**
 * A listener that binds to a port on the loopback interface. The listener
 * spawns {@link Receptor}s for each incoming connection.
 *
 * @author cilki
 * @since 5.0.0
 */
public class Listener implements Runnable, Closeable {

	/**
	 * A {@link RateLimiter} that limits connection attempts to prevent abuse.
	 */
	private final RateLimiter connectionLimiter = RateLimiter.create(4);

	/**
	 * The listening socket.
	 */
	private ServerSocket socket;

	/**
	 * The listening task's {@link Future}.
	 */
	private Future<?> future;

	/**
	 * The {@link ExecutorService} for spawned {@link Receptor}s.
	 */
	private ExecutorService receptorService;

	/**
	 * Create a new IPC listener on an ephemeral port.
	 *
	 * @throws IOException
	 */
	public Listener() throws IOException {
		this(0);
	}

	/**
	 * Create a new IPC listener on the specified port. A new socket will be bound
	 * to the port immediately, but connections will not be accepted until the
	 * listener is started.
	 *
	 * @param port The listening port
	 * @throws IOException
	 */
	public Listener(int port) throws IOException {
		socket = new ServerSocket(port, 4, InetAddress.getLoopbackAddress());
	}

	/**
	 * Get the listener's local port.
	 *
	 * @return The local port number
	 */
	public int getPort() {
		return socket.getLocalPort();
	}

	/**
	 * Start the listener on the given {@link ExecutorService}.
	 *
	 * @param listenerService The listener executor service
	 * @param receptorService The receptor executor service
	 */
	public void start(ExecutorService listenerService, ExecutorService receptorService) {
		if (future != null)
			throw new IllegalStateException("Listener already running");
		if (!socket.isBound())
			throw new IllegalStateException("Socket not bound");

		this.receptorService = Objects.requireNonNull(receptorService);

		future = listenerService.submit(this);
	}

	@Override
	public void close() throws IOException {
		if (socket != null)
			socket.close();
		if (future != null)
			future.cancel(true);
	}

	@Override
	public void run() {
		try {
			while (!socket.isClosed()) {
				connectionLimiter.acquire();

				Receptor receptor = new Receptor(socket.accept());
				receptor.start(receptorService);
				IPCStore.getMutableReceptors().add(receptor);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			IPCStore.getMutableListeners().remove(this);
		}
	}
}
