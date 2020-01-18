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

public class StatusBar extends Panel {

	private Label lbl_clients;
	private Label lbl_listeners;
	private Label lbl_about;

	private Label lbl_upload;
	private Label lbl_download;

	private Thread updater;

	public StatusBar() {
		super(new BorderLayout());

		{
			Panel controls = new Panel(new LinearLayout(Direction.HORIZONTAL));

			lbl_clients = new Label("[F1] Clients");
			controls.addComponent(lbl_clients);

			lbl_listeners = new Label("[F2] Listeners");
			controls.addComponent(lbl_listeners);

			lbl_about = new Label("[F3] About");
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
			var counter = sock.getTrafficInfo();
			updater = new Thread(() -> {
				while (!Thread.currentThread().isInterrupted()) {

					lbl_upload.setText("[UP: " + counter.lastWriteThroughput() + " b/s]");
					lbl_download.setText("[DN: " + counter.lastReadThroughput() + " b/s]");
					try {
						Thread.sleep(4000);
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
		var panels = List.of(lbl_clients, lbl_listeners, lbl_about);
		panels.forEach(lbl -> {
			lbl.setBackgroundColor(null).removeStyle(BOLD);
		});

		panels.get(index).setBackgroundColor(TextColor.ANSI.CYAN).addStyle(BOLD);
	}
}
