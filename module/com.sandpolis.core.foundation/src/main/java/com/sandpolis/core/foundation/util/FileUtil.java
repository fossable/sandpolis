//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.foundation.util;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.module.ModuleFinder;
import java.nio.BufferOverflowException;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;

/**
 * Utilities for manipulating files.
 *
 * @since 3.0.0
 */
public final class FileUtil {

	/**
	 * Copies the source file or directory to the destination file or directory.
	 * This method should only be used for small copy jobs.
	 *
	 * @param source The source file or directory
	 * @param dest   The destination file or directory
	 * @throws IOException
	 */
	public static void copy(File source, File dest) throws IOException {
		Objects.requireNonNull(source);
		Objects.requireNonNull(dest);
		if (!source.exists())
			throw new FileNotFoundException();

		copyRecursive(source, dest);
	}

	private static void copyRecursive(File source, File dest) throws IOException {
		if (source.isFile()) {
			if (dest.isFile())
				Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
			else
				Files.copy(source.toPath(), Paths.get(dest.getAbsolutePath(), source.getName()),
						StandardCopyOption.COPY_ATTRIBUTES);
		} else {
			if (!dest.exists())
				dest.mkdir();
			else if (!dest.isDirectory()) {
				throw new IllegalArgumentException("Cannot copy a directory to a file");
			}

			for (String child : source.list()) {
				copyRecursive(new File(source, child), new File(dest, child));
			}
		}
	}

	/**
	 * Locate a module's jar file in the given directory.
	 * 
	 * @param directory The directory to search
	 * @param module    The module name
	 * @return The file containing the desired module
	 */
	public static Optional<Path> findModule(Path directory, String module) {
		return ModuleFinder.of(directory).find(module).flatMap(ref -> {
			return ref.location().map(Paths::get);
		});
	}

	/**
	 * Logically overwrite a file with 0's. There's no way to know whether the new
	 * bytes will be written to the file's original physical location, so this
	 * method should not be used for secure applications.
	 *
	 * @param file The file to overwrite
	 * @throws IOException
	 */
	public static void overwrite(File file) throws IOException {
		Objects.requireNonNull(file);
		if (!file.exists())
			throw new FileNotFoundException();

		byte[] zeros = new byte[4096];

		try (RandomAccessFile raf = new RandomAccessFile(file, "w")) {
			for (long i = 0; i < raf.length(); i += zeros.length)
				raf.write(zeros);
			for (long i = 0; i < raf.length() % zeros.length; i++)
				raf.writeByte(0);
		}
	}

	/**
	 * Replace the first occurrence of the placeholder in the binary file with the
	 * given replacement. This method uses a standard needle/haystack linear search
	 * algorithm with backtracking.
	 * 
	 * @param binary      The binary file to process
	 * @param placeholder The unique placeholder
	 * @param replacement The payload buffer
	 * @throws IOException
	 */
	public static void replace(Path binary, short[] placeholder, byte[] replacement) throws IOException {

		// Check the replacement buffer size
		if (replacement.length > placeholder.length)
			throw new BufferOverflowException();

		try (var ch = FileChannel.open(binary, READ, WRITE)) {
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
				return;
			}

			// Placeholder not found
			throw new IOException("Failed to find placeholder");
		}
	}

	private FileUtil() {
	}
}
