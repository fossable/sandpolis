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
import java.net.InetSocketAddress;
import java.net.Socket;

import com.sandpolis.core.ipc.store.IPCStore;
import com.sandpolis.core.proto.ipc.MCMetadata.RQ_Metadata;
import com.sandpolis.core.proto.ipc.MCMetadata.RS_Metadata;
import com.sandpolis.core.proto.ipc.MSG.Message;

/**
 * A very simple IPC connection.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class Connector implements AutoCloseable {

	/**
	 * The connection timeout which can be set low since the destination is always a
	 * local socket.
	 */
	public static final int CONNECT_TIMEOUT = 200;

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
		this.socket = new Socket();
		this.socket.connect(new InetSocketAddress("127.0.0.1", port), CONNECT_TIMEOUT);
		this.in = socket.getInputStream();
		this.out = socket.getOutputStream();

		IPCStore.add(this);
	}

	@Override
	public void close() throws Exception {
		IPCStore.remove(this);

		socket.close();
	}

	/**
	 * Request instance metadata from the other endpoint of this connection.
	 * 
	 * @return The endpoint's metadata object
	 * @throws MessageFlowException
	 * @throws IOException
	 */
	public RS_Metadata rq_metadata() throws IOException {

		Message rq = Message.newBuilder().setRqMetadata(RQ_Metadata.newBuilder()).build();
		rq.writeDelimitedTo(out);

		Message message = Message.parseDelimitedFrom(in);
		if (message == null)
			return null;
		return message.getRsMetadata();
	}

}
