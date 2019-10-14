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
package com.sandpolis.plugin.desktop.client.mega;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.Raster;
import java.util.Arrays;

public class JavaDesktopSource {

	private static final int BLOCK_HEIGHT = 16;
	private static final int BLOCK_WIDTH = 16;

	private Robot robot;

	private int[][] hashcode;

	private int[][][] buffer;

	private Rectangle captureArea;

	public JavaDesktopSource() {
		try {
			robot = new Robot();
		} catch (AWTException e) {
			throw new RuntimeException(e);
		}
		captureArea = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		buffer = new int[captureArea.height / BLOCK_HEIGHT][captureArea.width / BLOCK_WIDTH][BLOCK_WIDTH
				* BLOCK_HEIGHT];
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

					// TODO convert block to output
				}
			}
		}
	}
}
