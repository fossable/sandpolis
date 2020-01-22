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
package com.sandpolis.viewer.ascetic.view.main;

import static com.googlecode.lanterna.SGR.BOLD;
import static com.sandpolis.core.net.store.connection.ConnectionStore.ConnectionStore;

import java.util.List;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.sandpolis.core.instance.Config;
import com.sandpolis.core.util.TextUtil;

public class StatusBar extends Panel {

	private Label lbl_clients;
	private Label lbl_listeners;
	private Label lbl_generator;
	private Label lbl_about;

	private Label lbl_upload;
	private Label lbl_download;

	private Thread updater;

	public StatusBar() {
		super(new BorderLayout());
		setFillColorOverride(TextColor.ANSI.GREEN);

		{
			Panel controls = new Panel(new LinearLayout(Direction.HORIZONTAL));

			lbl_clients = new Label("[F1] Clients");
			controls.addComponent(lbl_clients);

			lbl_listeners = new Label("[F2] Listeners");
			controls.addComponent(lbl_listeners);

			lbl_generator = new Label("[F3] Generator");
			controls.addComponent(lbl_generator);

			lbl_about = new Label("[F4] About");
			controls.addComponent(lbl_about);

			addComponent(controls, BorderLayout.Location.CENTER);
		}

		{
			Panel speeds = new Panel(new LinearLayout(Direction.HORIZONTAL));

			lbl_upload = new Label("");
			speeds.addComponent(lbl_upload);
			lbl_download = new Label("");
			speeds.addComponent(lbl_download);

			addComponent(speeds, BorderLayout.Location.RIGHT);
		}

		ConnectionStore.stream().findAny().ifPresent(sock -> {
			updater = new Thread(() -> {
				var counter = sock.getTrafficInfo();
				int timeout = Config.getInteger("traffic.interval");

				while (!Thread.currentThread().isInterrupted()) {

					lbl_upload.setText("[UP: " + TextUtil.formatByteCount(counter.lastWriteThroughput()) + "/s]");
					lbl_download.setText("[DN: " + TextUtil.formatByteCount(counter.lastReadThroughput()) + "/s]");
					try {
						Thread.sleep(timeout);
					} catch (InterruptedException e) {
						return;
					}
				}
			});
			updater.start();
		});

		setSelected(0);
	}

	public void setSelected(int index) {
		var panels = List.of(lbl_clients, lbl_listeners, lbl_generator, lbl_about);
		panels.forEach(lbl -> {
			lbl.setBackgroundColor(null).removeStyle(BOLD);
		});

		panels.get(index).setBackgroundColor(TextColor.ANSI.CYAN).addStyle(BOLD);
	}
}
