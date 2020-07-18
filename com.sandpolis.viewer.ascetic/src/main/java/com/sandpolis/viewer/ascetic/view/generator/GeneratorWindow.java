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
package com.sandpolis.viewer.ascetic.view.generator;

import static com.googlecode.lanterna.gui2.GridLayout.Alignment.BEGINNING;
import static com.googlecode.lanterna.gui2.GridLayout.Alignment.CENTER;
import static com.sandpolis.viewer.ascetic.store.window.WindowStore.WindowStore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import com.googlecode.lanterna.gui2.GridLayout.Alignment;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.dialogs.FileDialogBuilder;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.sandpolis.core.instance.Generator.GenConfig;
import com.sandpolis.core.instance.Generator.LoopConfig;
import com.sandpolis.core.instance.Generator.MegaConfig;
import com.sandpolis.core.instance.Generator.NetworkConfig;
import com.sandpolis.core.instance.Generator.NetworkTarget;
import com.sandpolis.core.instance.Generator.OutputFormat;
import com.sandpolis.core.instance.Generator.OutputPayload;
import com.sandpolis.core.viewer.cmd.GenCmd;
import com.sandpolis.viewer.ascetic.Viewer;
import com.sandpolis.viewer.ascetic.renderer.CustomButtonRenderer;

public class GeneratorWindow extends BasicWindow {

	public static final Logger log = LoggerFactory.getLogger(Viewer.class);

	private TextBox fld_path;
	private TextBox fld_address;
	private TextBox fld_port;
	private TextBox fld_timeout;

	private Label lbl_status;

	private Button btn_choose;

	public GeneratorWindow() {
		setHints(Collections.singleton(Window.Hint.CENTERED));

		Panel content = new Panel(new GridLayout(1));

		{
			Panel server = new Panel(new GridLayout(4));

			Label lbl_address = new Label("Address");
			lbl_address.setForegroundColor(TextColor.Indexed.fromRGB(128, 128, 128));
			server.addComponent(lbl_address);

			Label lbl_port = new Label("Port");
			lbl_port.setForegroundColor(TextColor.Indexed.fromRGB(128, 128, 128));
			server.addComponent(lbl_port, GridLayout.createLayoutData(CENTER, BEGINNING, false, false));

			Label lbl_timeout = new Label("Timeout");
			lbl_timeout.setForegroundColor(TextColor.Indexed.fromRGB(128, 128, 128));
			server.addComponent(lbl_timeout, GridLayout.createLayoutData(CENTER, BEGINNING, false, false));
			server.addComponent(new EmptySpace(new TerminalSize(0, 1)));

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

			fld_timeout = new TextBox().setPreferredSize(new TerminalSize(7, 1))
					.setInputFilter((Interactable e, KeyStroke b) -> {
						if (b.getKeyType() == KeyType.Character) {
							return Character.isDigit(b.getCharacter());
						}
						return true;
					});
			server.addComponent(fld_timeout);
			server.addComponent(new Label("ms"));

			content.addComponent(server.withBorder(Borders.singleLine("Server")));
		}

		{
			Panel output = new Panel(new GridLayout(2));

			Label lbl_path = new Label("Path");
			lbl_path.setForegroundColor(TextColor.Indexed.fromRGB(128, 128, 128));
			output.addComponent(lbl_path, GridLayout.createLayoutData(BEGINNING, BEGINNING, false, false));
			output.addComponent(new EmptySpace());

			fld_path = new TextBox().setPreferredSize(new TerminalSize(39, 1));
			output.addComponent(fld_path);

			btn_choose = new Button("Choose", () -> {
				File file = new FileDialogBuilder().setTitle("Choose generator output").setActionLabel("OK").build()
						.showDialog(WindowStore.gui);
				if (file != null) {
					fld_path.setText(file.getAbsolutePath());
				}
			});
			output.addComponent(btn_choose);

			content.addComponent(output.withBorder(Borders.singleLine("File Output")),
					GridLayout.createLayoutData(BEGINNING, BEGINNING, true, false));
		}

		{
			lbl_status = new Label("Enter your server details above");
			content.addComponent(lbl_status);
		}

		{
			Panel buttons = new Panel(new BorderLayout());

			Button btn_exit = new Button("Cancel", () -> {
				close();
			});
			btn_exit.setRenderer(new CustomButtonRenderer());
			buttons.addComponent(btn_exit, BorderLayout.Location.LEFT);

			Button btn_generate = new Button("Generate", () -> {
				fld_path.setEnabled(false);

				var config = GenConfig.newBuilder().setPayload(OutputPayload.OUTPUT_MEGA).setFormat(OutputFormat.JAR)
						.setMega(MegaConfig.newBuilder()
								.setNetwork(NetworkConfig.newBuilder()
										.setLoopConfig(LoopConfig.newBuilder().setTimeout(5000)
												.setCooldown(Integer.parseInt(fld_timeout.getText()))
												.addTarget(NetworkTarget.newBuilder().setAddress(fld_address.getText())
														.setPort(Integer.parseInt(fld_port.getText()))))));

				setStatus("Generating...", TextColor.ANSI.BLACK);
				GenCmd.async().generate(config.build()).thenAccept(rs -> {
					fld_address.setEnabled(false);
					fld_path.setEnabled(false);
					fld_port.setEnabled(false);
					fld_timeout.setEnabled(false);
					btn_choose.setEnabled(false);

					if (rs.getReport().getResult()) {
						try {
							Files.write(Paths.get(fld_path.getText()), rs.getOutput().toByteArray());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						close();
					}
				});
			});
			btn_generate.setRenderer(new CustomButtonRenderer());
			buttons.addComponent(btn_generate, BorderLayout.Location.RIGHT);

			content.addComponent(new EmptySpace(new TerminalSize(0, 1)));
			content.addComponent(buttons, GridLayout.createLayoutData(Alignment.END, BEGINNING, true, false));
		}

		setComponent(content);
	}

	private void setStatus(String status, TextColor color) {
		lbl_status.setText(status);
		lbl_status.setForegroundColor(color);
	}
}
