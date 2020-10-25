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
package com.sandpolis.plugin.desktop.agent.vanilla;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.Raster;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import com.google.protobuf.ByteString;
import com.sandpolis.core.net.stream.StreamSource;
import com.sandpolis.plugin.desktop.msg.MsgRd.DirtyBlock;
import com.sandpolis.plugin.desktop.msg.MsgRd.EV_DesktopStream;
import com.sandpolis.plugin.desktop.msg.MsgRd.RQ_DesktopStream.ColorMode;

public class JavaDesktopSource extends StreamSource<EV_DesktopStream> {

	private static final int BLOCK_HEIGHT = 16;
	private static final int BLOCK_WIDTH = 16;

	private Robot robot;

	private int[][] hashcode;

	private int[][][] buffer;

	private ColorMode colorMode = ColorMode.RGB888;

	private Rectangle captureArea;

	public JavaDesktopSource() {
		try {
			robot = new Robot();
		} catch (AWTException e) {
			throw new RuntimeException(e);
		}
		captureArea = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		buffer = new int[captureArea.height / BLOCK_HEIGHT][captureArea.width / BLOCK_WIDTH][BLOCK_WIDTH * BLOCK_HEIGHT
				* 4];
		hashcode = new int[captureArea.height / BLOCK_HEIGHT][captureArea.width / BLOCK_WIDTH];
	}

	private void pump() {
		Raster raster = robot.createScreenCapture(captureArea).getData();

		for (int j = 0; j < buffer.length; j++) {
			for (int i = 0; i < buffer[j].length; i++) {
				raster.getPixels(i * BLOCK_WIDTH, j * BLOCK_HEIGHT, BLOCK_WIDTH, BLOCK_HEIGHT, buffer[j][i]);

				// Recompute hashcode
				int hash = Arrays.hashCode(buffer[j][i]);
				if (hashcode[j][i] != hash) {
					hashcode[j][i] = hash;

					try (var out = new ByteArrayOutputStream(); var data = new DataOutputStream(out)) {
						for (int v : buffer[j][i])
							data.writeInt(v);

						submit(EV_DesktopStream.newBuilder()
								.setDirtyBlock(DirtyBlock.newBuilder().setData(ByteString.copyFrom(out.toByteArray())))
								.build());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void start() {
		// Temporary
		new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				pump();
			}
		}).start();
	}
}
