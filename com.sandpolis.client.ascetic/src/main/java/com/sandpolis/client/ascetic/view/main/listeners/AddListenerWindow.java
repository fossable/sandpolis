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
package com.sandpolis.client.ascetic.view.main.listeners;

import static com.googlecode.lanterna.gui2.GridLayout.Alignment.BEGINNING;
import static com.sandpolis.client.ascetic.store.window.WindowStore.WindowStore;

import java.util.Collections;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.sandpolis.core.instance.Listener.ListenerConfig;
import com.sandpolis.core.client.cmd.ListenerCmd;

public class AddListenerWindow extends BasicWindow {

	private TextBox fld_name;
	private TextBox fld_port;

	private Label lbl_status;

	public AddListenerWindow() {
		setHints(Collections.singleton(Window.Hint.CENTERED));

		Panel content = new Panel(new GridLayout(1));

		{
			Panel listener = new Panel(new GridLayout(2));

			Label lbl_name = new Label("Name");
			listener.addComponent(lbl_name);

			Label lbl_port = new Label("Port");
			listener.addComponent(lbl_port);

			fld_name = new TextBox().setPreferredSize(new TerminalSize(30, 1));
			fld_name.setLayoutData(GridLayout.createLayoutData(BEGINNING, BEGINNING, true, false, 1, 1));
			listener.addComponent(fld_name);

			fld_port = new TextBox().setPreferredSize(new TerminalSize(6, 1))
					.setInputFilter((Interactable e, KeyStroke b) -> {
						if (b.getKeyType() == KeyType.Character) {
							return Character.isDigit(b.getCharacter()) && ((TextBox) e).getText().length() < 5;
						}
						return true;
					});
			listener.addComponent(fld_port);

			content.addComponent(listener.withBorder(Borders.singleLine("Add New Listener")));
		}

		{
			lbl_status = new Label("Enter listener details above");
			content.addComponent(lbl_status);
		}

		{
			Panel buttons = new Panel(new BorderLayout());

			Button btn_cancel = new Button("Cancel", this::close);
			buttons.addComponent(btn_cancel, BorderLayout.Location.LEFT);
			buttons.addComponent(new EmptySpace(new TerminalSize(23, 0)), BorderLayout.Location.CENTER);

			Button btn_add = new Button("Add", () -> {

				String name = fld_name.getText();
				int port = Integer.parseInt(fld_port.getText());

				ListenerCmd.async().create(ListenerConfig.newBuilder().setName(name).setPort(port).setOwner("admin")
						.setAddress("0.0.0.0").setEnabled(true).build()).thenAccept(rs -> {
							if (rs.getResult()) {
								WindowStore.removeValue(this);
							} else {
								setStatus("Add failed", TextColor.ANSI.BLACK);
							}
						});
			});
			buttons.addComponent(btn_add, BorderLayout.Location.RIGHT);

			content.addComponent(buttons.withBorder(Borders.singleLineBevel()));
		}

		setComponent(content);
	}

	private void setStatus(String status, TextColor color) {
		lbl_status.setText(status);
		lbl_status.setForegroundColor(color);
	}
}
