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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.google.common.util.concurrent.RateLimiter;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.ipc.store.IPCStore;
import com.sandpolis.core.proto.ipc.MCMetadata.RS_Metadata;
import com.sandpolis.core.proto.ipc.MSG.Message;

/**
 * This simple IPC receiver processes messages serially from a {@code Socket}.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class Receptor extends Thread {

	/**
	 * The connection's socket.
	 */
	private Socket socket;

	/**
	 * A {@link RateLimiter} that limits message processing to prevent abuse.
	 * Connections that exceed the limit are closed.
	 */
	private final RateLimiter messageLimiter = RateLimiter.create(4);

	/**
	 * Create a new IPC Receptor around an established socket. The Receptor
	 * immediately begins listening on the socket.
	 * 
	 * @param socket
	 *            A Socket pre-established by an IPC Listener.
	 */
	public Receptor(Socket socket) {
		this.socket = socket;

		start();
		IPCStore.add(this);
	}

	@Override
	public void run() {
		try (Socket s = socket; InputStream in = socket.getInputStream(); OutputStream out = socket.getOutputStream()) {
			Message message;
			while (!Thread.currentThread().isInterrupted()) {
				message = Message.parseDelimitedFrom(in);
				if (message == null)
					return;
				if (!messageLimiter.tryAcquire())
					// Limit exceeded; close the connection
					return;

				switch (message.getMsgCase()) {
				case RQ_METADATA:
					Message.newBuilder()
							.setRsMetadata(RS_Metadata.newBuilder().setInstance(Core.INSTANCE)
									.setVersion(Core.SO_BUILD.getVersion()).setPid(ProcessHandle.current().pid()))
							.build().writeDelimitedTo(out);
					break;
				default:
					break;

				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			// Remove this receptor
			IPCStore.remove(this);
		}

	}

}
