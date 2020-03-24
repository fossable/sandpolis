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
package com.sandpolis.plugin.filesys.util;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;

import com.google.protobuf.ByteString;
import com.sandpolis.plugin.filesys.MsgFilesys.RS_FileInfo;

public final class FileUtil {

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

	public static RS_FileInfo getFileInfo(String path) {
		File file = new File(Objects.requireNonNull(path));

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

	private FileUtil() {
	}
}
