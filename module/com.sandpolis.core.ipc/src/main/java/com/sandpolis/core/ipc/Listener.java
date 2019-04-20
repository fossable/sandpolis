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
package com.sandpolis.core.ipc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import com.google.common.util.concurrent.RateLimiter;
import com.sandpolis.core.ipc.store.IPCStore;

/**
 * An IPC listener which binds to a port on the loopback interface.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class Listener extends Thread {

	/**
	 * Maximum capacity of the incoming connection queue.
	 */
	private static final int BACKLOG = 4;

	/**
	 * The listening socket.
	 */
	private ServerSocket socket;

	/**
	 * A {@link RateLimiter} that limits connection attempts to prevent abuse.
	 */
	private final RateLimiter connectionLimiter = RateLimiter.create(4);

	/**
	 * Create a new IPC listener on the specified port.
	 * 
	 * @param port
	 *            The port
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public Listener(int port) throws UnknownHostException, IOException {
		this.socket = new ServerSocket(port, BACKLOG, InetAddress.getLoopbackAddress());

		start();
	}

	@Override
	public void run() {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				connectionLimiter.acquire();
				new Receptor(socket.accept());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IPCStore.remove(this);
		}
	}

}
