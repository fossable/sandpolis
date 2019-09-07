/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.core.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Utilities for manipulating files.
 *
 * @author cilki
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

		recursiveCopy(source, dest);
	}

	private static void recursiveCopy(File source, File dest) throws IOException {
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
				recursiveCopy(new File(source, child), new File(dest, child));
			}
		}
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

	private FileUtil() {
	}
}
