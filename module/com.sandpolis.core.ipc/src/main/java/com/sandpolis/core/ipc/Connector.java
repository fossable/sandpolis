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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;

import com.sandpolis.core.foundation.Config;
import com.sandpolis.core.ipc.Message.MSG;
import com.sandpolis.core.ipc.Metadata.RQ_Metadata;
import com.sandpolis.core.ipc.Metadata.RS_Metadata;

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
		socket.connect(new InetSocketAddress("127.0.0.1", port), Config.IPC_TIMEOUT.value().orElse(5000));

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
			MSG.newBuilder().setRqMetadata(RQ_Metadata.newBuilder()).build().writeDelimitedTo(out);

			MSG message = MSG.parseDelimitedFrom(in);
			if (message == null)
				return Optional.empty();

			return Optional.of(message.getRsMetadata());
		}
	}
}
