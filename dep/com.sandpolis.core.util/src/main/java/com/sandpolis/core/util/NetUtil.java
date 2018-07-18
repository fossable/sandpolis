/******************************************************************************
 *                                                                            *
 *                    Copyright 2016 Subterranean Security                    *
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
package com.sandpolis.core.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;

/**
 * Network utilities.
 * 
 * @author cilki
 * @since 3.0.0
 */
public class NetUtil {

	/**
	 * The maximum number of bytes that can be downloaded with
	 * {@link #download(String)} or {@link #download(URL)}. This limit exists to
	 * prevent out of memory errors.
	 */
	private static final int DOWNLOAD_LIMIT = 512 * 1024 * 1024 * 1024;

	/**
	 * The number of milliseconds to wait before considering the port to be closed.
	 */
	public static final int PORTCHECK_TIMEOUT = 850;

	/**
	 * Download a file from the Internet. The entire file is loaded into memory, so
	 * only use with small files!
	 * 
	 * @param url The resource location
	 * @return A byte array containing the file
	 * @throws IOException
	 */
	public static byte[] download(String url) throws IOException {
		return download(new URL(url));
	}

	/**
	 * Download a file from the Internet. The entire file is loaded into memory, so
	 * only use with small files!
	 * 
	 * @param url The resource location
	 * @return A byte array containing the file
	 * @throws IOException
	 */
	public static byte[] download(URL url) throws IOException {
		if (url == null)
			throw new IllegalArgumentException();

		URLConnection con = url.openConnection();

		if (con.getContentLength() > DOWNLOAD_LIMIT)
			throw new IllegalArgumentException("File too large");

		try (DataInputStream in = new DataInputStream(con.getInputStream())) {
			return in.readAllBytes();
		}

	}

	/**
	 * Download a file from the Internet to a local file. This method has no size
	 * limits.
	 * 
	 * @param url  The resource location
	 * @param file The output file
	 * @throws IOException
	 */
	public static void download(String url, File file) throws IOException {
		download(new URL(url), file);
	}

	/**
	 * Download a file from the Internet to a local file. This method has no size
	 * limits.
	 * 
	 * @param url  The resource location
	 * @param file The output file
	 * @throws IOException
	 */
	public static void download(URL url, File file) throws IOException {
		if (url == null)
			throw new IllegalArgumentException();
		if (file == null)
			throw new IllegalArgumentException();

		URLConnection con = url.openConnection();

		try (DataInputStream in = new DataInputStream(con.getInputStream())) {
			try (FileOutputStream out = new FileOutputStream(file)) {
				in.transferTo(out);
			}
		}
	}

	/**
	 * Tests the visibility of a port on a remote host by attempting a socket
	 * connection.
	 * 
	 * @param host The target DNS name or IP address
	 * @param port The target port
	 * @return True if the port is open, false if the port is closed or there was a
	 *         connection error
	 */
	public static boolean checkPort(String host, int port) {
		if (!DomainValidator.getInstance().isValid(host) && !InetAddressValidator.getInstance().isValid(host))
			throw new IllegalArgumentException("Invalid host: " + host);
		if (!ValidationUtil.port(port))
			throw new IllegalArgumentException("Invalid port: " + port);

		try (Socket sock = new Socket()) {
			sock.connect(new InetSocketAddress(host, port), PORTCHECK_TIMEOUT);
			return sock.isConnected();
		} catch (UnknownHostException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
	}

}