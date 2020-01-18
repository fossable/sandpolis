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
package com.sandpolis.viewer.ascetic.view.login;

import static com.googlecode.lanterna.gui2.GridLayout.Alignment.BEGINNING;
import static com.googlecode.lanterna.gui2.GridLayout.Alignment.CENTER;
import static com.sandpolis.core.net.store.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.viewer.ascetic.store.window.WindowStore.WindowStore;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.googlecode.lanterna.gui2.LayoutData;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.net.future.SockFuture;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.viewer.cmd.LoginCmd;
import com.sandpolis.viewer.ascetic.Viewer;
import com.sandpolis.viewer.ascetic.view.main.MainWindow;

public class LoginWindow extends BasicWindow {

	public static final Logger log = LoggerFactory.getLogger(Viewer.class);

	private TextBox fld_username;
	private TextBox fld_password;
	private TextBox fld_address;
	private TextBox fld_port;

	private Label lbl_status;

	public LoginWindow() {
		init();

		if (Core.SO_BUILD.getDevelopment()) {
			fld_username.setText("admin");
			fld_password.setText("password");
			fld_address.setText("127.0.0.1");
			fld_port.setText("8768");
		}
	}

	private void init() {
		setHints(Collections.singleton(Window.Hint.CENTERED));

		Panel content = new Panel(new GridLayout(1));
		LayoutData labelData = GridLayout.createLayoutData(CENTER, BEGINNING, false, false);

		{
			Panel server = new Panel(new GridLayout(2));

			Label lbl_address = new Label("Address");
			lbl_address.setForegroundColor(TextColor.Indexed.fromRGB(128, 128, 128));
			server.addComponent(lbl_address);

			Label lbl_port = new Label("Port").setLayoutData(labelData);
			lbl_port.setForegroundColor(TextColor.Indexed.fromRGB(128, 128, 128));
			server.addComponent(lbl_port);

			fld_address = new TextBox().setPreferredSize(new TerminalSize(30, 1));
			fld_address.setLayoutData(GridLayout.createLayoutData(BEGINNING, BEGINNING, true, false, 1, 1));
			server.addComponent(fld_address);

			fld_port = new TextBox().setPreferredSize(new TerminalSize(6, 1))
					.setInputFilter((Interactable e, KeyStroke b) -> {
						if (b.getKeyType() == KeyType.Character) {
							return Character.isDigit(b.getCharacter()) && ((TextBox) e).getText().length() < 5;
						}
						return true;
					});
			server.addComponent(fld_port);

			content.addComponent(server.withBorder(Borders.singleLine("Server")));
		}

		{
			Panel credentials = new Panel(new GridLayout(2).setHorizontalSpacing(5));

			Label lbl_username = new Label("Username").setLayoutData(labelData);
			lbl_username.setForegroundColor(TextColor.Indexed.fromRGB(128, 128, 128));
			credentials.addComponent(lbl_username);

			Label lbl_password = new Label("Password").setLayoutData(labelData);
			lbl_password.setForegroundColor(TextColor.Indexed.fromRGB(128, 128, 128));
			credentials.addComponent(lbl_password);

			fld_username = new TextBox().setPreferredSize(new TerminalSize(16, 1));
			credentials.addComponent(fld_username);

			fld_password = new TextBox().setPreferredSize(new TerminalSize(16, 1)).setMask('*');
			credentials.addComponent(fld_password);

			content.addComponent(credentials.withBorder(Borders.singleLine("Credentials")));
		}

		{
			lbl_status = new Label("Enter your server details above");
			content.addComponent(lbl_status);
		}

		{
			Panel buttons = new Panel(new BorderLayout());

			Button btn_exit = new Button("Exit", () -> {
				close();
				System.exit(0);
			});
			buttons.addComponent(btn_exit, BorderLayout.Location.LEFT);
			buttons.addComponent(new EmptySpace(new TerminalSize(23, 0)), BorderLayout.Location.CENTER);

			Button btn_login = new Button("Login", () -> {
				fld_username.setEnabled(false);
				fld_password.setEnabled(false);

				String address = fld_address.getText();
				int port = Integer.parseInt(fld_port.getText());
				String username = fld_username.getText();
				String password = fld_password.getText();

				setStatus("Establishing connection...", TextColor.ANSI.BLACK);
				ConnectionStore.connect(address, port, false).addListener((SockFuture sockFuture) -> {
					if (sockFuture.isSuccess()) {
						setStatus("Logging in...", TextColor.ANSI.BLACK);
						LoginCmd.async().target(sockFuture.get()).login(username, password)
								.addHandler((Outcome outcome) -> {
									if (outcome.getResult()) {
										WindowStore.clear();
										WindowStore.add(new MainWindow());
									} else {
										fld_username.setEnabled(true);
										fld_password.setEnabled(true);

										setStatus("Login attempt failed!", TextColor.ANSI.RED);
									}
								});
					} else {
						fld_username.setEnabled(true);
						fld_password.setEnabled(true);

						setStatus("Connection attempt failed!", TextColor.ANSI.RED);
					}
				});
			});
			buttons.addComponent(btn_login, BorderLayout.Location.RIGHT);

			content.addComponent(buttons.withBorder(Borders.singleLineBevel()));
		}

		setComponent(content);
	}

	private void setStatus(String status, TextColor color) {
		lbl_status.setText(status);
		lbl_status.setForegroundColor(color);
	}
}
