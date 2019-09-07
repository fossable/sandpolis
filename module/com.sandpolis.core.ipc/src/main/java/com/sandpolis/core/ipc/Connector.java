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

import static com.sandpolis.core.ipc.IPCStore.IPCStore;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.ConfigConstant.net;
import com.sandpolis.core.ipc.MCMetadata.RQ_Metadata;
import com.sandpolis.core.ipc.MCMetadata.RS_Metadata;
import com.sandpolis.core.ipc.MSG.Message;

/**
 * A simple IPC connection.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class Connector implements Closeable {

	/**
	 * The underlying socket.
	 */
	private Socket socket;

	/**
	 * The socket's input stream.
	 */
	private InputStream in;

	/**
	 * The socket's output stream.
	 */
	private OutputStream out;

	public Connector(int port) throws IOException {
		socket = new Socket();
		socket.connect(new InetSocketAddress("127.0.0.1", port), Config.getInteger(net.ipc.timeout));

		if (socket.isConnected()) {
			in = socket.getInputStream();
			out = socket.getOutputStream();

			IPCStore.getMutableConnectors().add(this);
		}
	}

	@Override
	public void close() throws IOException {
		socket.close();

		IPCStore.getMutableConnectors().remove(this);
	}

	/**
	 * Request instance metadata from the other endpoint of this connection.
	 * 
	 * @return The endpoint's metadata object
	 * @throws IOException
	 */
	public Optional<RS_Metadata> rq_metadata() throws IOException {
		if (socket.isClosed())
			throw new IOException("Closed socket");

		synchronized (socket) {
			Message.newBuilder().setRqMetadata(RQ_Metadata.newBuilder()).build().writeDelimitedTo(out);

			Message message = Message.parseDelimitedFrom(in);
			if (message == null)
				return Optional.empty();

			return Optional.of(message.getRsMetadata());
		}
	}
}
