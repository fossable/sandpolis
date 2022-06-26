//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.module.ModuleFinder;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public record S7SFile(Path path) {

	public static S7SFile of(Path path) {
		return new S7SFile(path);
	}

	public static S7SFile of(File file) {
		return new S7SFile(file.toPath());
	}

	public static S7SFile of(String file) {
		return new S7SFile(Paths.get(file));
	}

	/**
	 * Attempt to locate an executable on the PATH.
	 *
	 * @param executable The name to find
	 * @return The executable if found
	 */
	public static Optional<S7SFile> which(String executable) {

		// Search PATH environment variable or default
		switch (S7SSystem.OS_TYPE) {
		case MACOS:
		case LINUX:
			for (var path : S7SEnvironmentVariable.of("PATH").value().orElse("/usr/bin:/bin:/usr/sbin:/usr/local/bin")
					.split(":")) {
				if (Files.isExecutable(Paths.get(path).resolve(executable))) {
					return Optional.of(S7SFile.of(Paths.get(path).resolve(executable)));
				}
			}
			break;
		case WINDOWS:
			for (var path : S7SEnvironmentVariable.of("PATH").value().orElse("C:\\Windows\\system32;C:\\Windows")
					.split(";")) {
				if (Files.isExecutable(Paths.get(path).resolve(executable))) {
					return Optional.of(S7SFile.of(Paths.get(path).resolve(executable)));
				}
			}
			break;
		default:
			break;
		}

		return Optional.empty();
	}

	/**
	 * Locate a module's jar file in the given directory.
	 *
	 * @param module The module name
	 * @return The file containing the desired module
	 */
	public Optional<Path> findModule(String module) {

		if (!Files.isDirectory(path))
			throw new IllegalArgumentException();

		return ModuleFinder.of(path).find(module).flatMap(ref -> {
			return ref.location().map(Paths::get);
		});
	}

	/**
	 * Download a file from the Internet to a local file.
	 *
	 * @param url  The resource location
	 * @param file The output file
	 * @return this
	 * @throws IOException
	 */
	public S7SFile download(String url) throws IOException {
		return download(new URL(url));
	}

	/**
	 * Download a file from the Internet to a local file.
	 *
	 * @param url  The resource location
	 * @param file The output file
	 * @return this
	 * @throws IOException
	 */
	public S7SFile download(URL url) throws IOException {

		if (url == null)
			throw new IllegalArgumentException();

		URLConnection con = url.openConnection();

		try (DataInputStream in = new DataInputStream(con.getInputStream())) {
			try (OutputStream out = Files.newOutputStream(path)) {
				in.transferTo(out);
			}
		}

		return this;
	}

	/**
	 * Logically overwrite a file with 0's. There's no way to know whether the new
	 * bytes will be written to the file's original physical location, so this
	 * method should not be used for secure applications.
	 *
	 * @return this
	 * @throws IOException
	 */
	public S7SFile overwrite() throws IOException {

		if (!Files.exists(path))
			throw new FileNotFoundException();

		byte[] zeros = new byte[4096];

		try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rws")) {
			if (raf.length() < zeros.length) {
				raf.write(new byte[(int) raf.length()]);
				return this;
			}

			for (long i = 0; i < raf.length(); i += zeros.length) {
				raf.write(zeros);
			}
			for (long i = 0; i < raf.length() % zeros.length; i++) {
				raf.writeByte(0);
			}
		}

		return this;
	}

	/**
	 * Replace the first occurrence of the placeholder in the binary file with the
	 * given replacement. This method uses a standard needle/haystack linear search
	 * algorithm with backtracking.
	 *
	 * @param placeholder The unique placeholder
	 * @param replacement The payload buffer
	 * @return this
	 * @throws IOException
	 */
	public S7SFile replace(short[] placeholder, byte[] replacement) throws IOException {

		if (!Files.exists(path))
			throw new FileNotFoundException();

		// Check the replacement buffer size
		if (replacement.length > placeholder.length)
			throw new IllegalArgumentException("The replacement cannot be larger than the placeholder");

		try (var ch = FileChannel.open(path, READ, WRITE)) {
			var buffer = ch.map(MapMode.READ_WRITE, ch.position(), ch.size()).order(ByteOrder.nativeOrder());

			buffer.mark();
			find: while (buffer.remaining() >= placeholder.length) {
				for (int i = 0; i < placeholder.length; i++) {
					if (buffer.get() != (byte) placeholder[i]) {
						buffer.reset();
						buffer.position(buffer.position() + 1).mark();
						continue find;
					}
				}

				// Return to the start of the placeholder
				buffer.position(buffer.position() - placeholder.length);

				// Overwrite
				buffer.put(replacement);
				return this;
			}

			// Placeholder not found
			return this;
		}
	}
}
