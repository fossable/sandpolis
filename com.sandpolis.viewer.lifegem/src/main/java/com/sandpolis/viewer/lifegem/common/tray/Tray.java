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
package com.sandpolis.viewer.lifegem.common.tray;

import static com.sandpolis.viewer.lifegem.stage.StageStore.StageStore;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * A singleton for interacting with the AWT system tray.
 *
 * @author cilki
 * @since 2.0.0
 */
public final class Tray {

	/**
	 * The current system tray.
	 */
	private static TrayIcon tray;

	/**
	 * Attempt to move the application to the system tray if supported.
	 *
	 * @throws AWTException
	 */
	public static synchronized void background() throws AWTException {
		if (tray != null)
			throw new IllegalStateException();
		if (!isSupported())
			throw new UnsupportedOperationException();

		MenuItem restoreItem = new MenuItem("Restore");
		restoreItem.addActionListener(event -> foreground());

		MenuItem exitItem = new MenuItem("Exit");
		exitItem.addActionListener(event -> System.exit(0));

		PopupMenu popup = new PopupMenu();
		popup.add(restoreItem);
		popup.add(exitItem);

		try {
			tray = new TrayIcon(ImageIO.read(Tray.class.getResourceAsStream("/image/icon16/common/crimson.png")),
					"Sandpolis Viewer", popup);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		tray.setImageAutoSize(true);

		SystemTray.getSystemTray().add(tray);

		// Hide interface
		StageStore.hideAll();
	}

	/**
	 * Attempt to move the application out of the system tray.
	 */
	public static void foreground() {
		if (tray == null)
			throw new IllegalStateException();
		if (!isSupported())
			throw new UnsupportedOperationException();

		SystemTray.getSystemTray().remove(tray);
		tray = null;

		// Restore interface
		StageStore.showAll();
	}

	/**
	 * Get whether the system tray is supported.
	 *
	 * @return Whether the system tray is supported.
	 */
	public static boolean isSupported() {
		return SystemTray.isSupported();
	}

	/**
	 * Get whether the application is currently backgrounded.
	 *
	 * @return Whether the application is running in the background
	 */
	public static boolean isBackgrounded() {
		return tray != null;
	}

	private Tray() {
	}
}
