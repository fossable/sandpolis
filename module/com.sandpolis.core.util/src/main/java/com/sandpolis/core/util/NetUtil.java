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
package com.sandpolis.core.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;

/**
 * Networking utilities for tasks like downloading files from the Internet,
 * checking for open ports, resolving IANA service names, etc.
 *
 * @author cilki
 * @since 3.0.0
 */
public final class NetUtil {

	/**
	 * The maximum number of bytes that can be downloaded directly into memory with
	 * {@link #download(String)} or {@link #download(URL)}. This limit exists to
	 * prevent memory issues.
	 */
	private static final long DOWNLOAD_LIMIT = 536870912L; // 512 MiB

	/**
	 * The number of milliseconds to wait before considering a port to be
	 * unreachable.
	 */
	private static final int PORTCHECK_TIMEOUT = 850;

	/**
	 * A cache for service names obtained from the system.
	 */
	private static Map<Integer, String> serviceCache = new HashMap<>();

	/**
	 * Whether service name resolution is enabled. This flag will be set if it
	 * becomes unlikely that future resolution attempts will succeed.
	 */
	private static boolean serviceResolutionEnabled = true;

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
		long size = con.getContentLengthLong();

		if (size > DOWNLOAD_LIMIT)
			throw new IllegalArgumentException("File too large: " + size + " bytes");

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
	 * Resolve an IANA service name.
	 *
	 * @param port A TCP port number
	 * @return The service name registered to the given port number
	 */
	public static Optional<String> serviceName(int port) {
		if (!ValidationUtil.port(port))
			throw new IllegalArgumentException("Invalid port: " + port);

		if (!serviceResolutionEnabled)
			// Service name resolution disabled
			return Optional.empty();

		if (port >= 49152)
			// Ephemeral port range
			return Optional.empty();

		if (serviceCache.containsKey(port))
			// The service name is cached
			return Optional.of(serviceCache.get(port));

		try {
			var serviceName = serviceName0(port);
			if (serviceName != null) {
				serviceCache.put(port, serviceName);
				return Optional.of(serviceName);
			} else {
				return Optional.empty();
			}
		} catch (Exception e) {
			serviceResolutionEnabled = false;
			return Optional.empty();
		}
	}

	private static String serviceName0(int port) throws IOException {

		Path services = Paths.get("/etc/services");
		if (Files.exists(services)) {
			try (var in = Files.newBufferedReader(services)) {

				// Skip header
				in.readLine();
				in.readLine();

				String line;
				while ((line = in.readLine()) != null) {
					int p = Integer.parseInt(line.substring(16, 21).trim());

					if (p > port) {
						break;
					} else if (p == port) {
						return line.substring(0, 16).trim();
					}
				}
				return null;
			}
		}

		// TODO check other locations
		throw new FileNotFoundException();
	}

	private NetUtil() {
	}
}
