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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;

import com.google.protobuf.ByteString;
import com.sandpolis.core.proto.net.MCFsHandle.RS_FileInfo;

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

	public static RS_FileInfo getFileInfo(String path) {
		Objects.requireNonNull(path);

		File file = new File(path);

		RS_FileInfo.Builder rs = RS_FileInfo.newBuilder();
		try {
			rs.setLocalIcon(ByteString.copyFrom(getFileIcon(file)));
		} catch (IOException e) {
			rs.clearLocalIcon();
		}
		rs.setName(file.getName());
		rs.setPath(file.getParent());
		rs.setSize(file.length());
		rs.setMtime(file.lastModified());

		return rs.build();
	}

	/**
	 * Get a file's default icon from the system.
	 * 
	 * @param file A file
	 * @return The file's default icon
	 * @throws IOException
	 */
	public static byte[] getFileIcon(File file) throws IOException {
		Objects.requireNonNull(file);

		Icon icon = FileSystemView.getFileSystemView().getSystemIcon(file);
		BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = image.createGraphics();
		icon.paintIcon(null, g, 0, 0);
		g.dispose();

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			ImageIO.write(image, "png", out);
			return out.toByteArray();
		}
	}

	private FileUtil() {
	}
}