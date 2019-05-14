/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
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
package com.sandpolis.installer.scene.main;

import com.sandpolis.core.instance.PlatformUtil;
import com.sandpolis.installer.install.AbstractInstaller;
import com.sandpolis.installer.install.LinuxInstaller;
import com.sandpolis.installer.install.WindowsInstaller;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.Clipboard;

public class MainController {

	@FXML
	private CheckBox chk_server;

	@FXML
	private CheckBox chk_viewer_jfx;

	@FXML
	private CheckBox chk_viewer_cli;

	@FXML
	private CheckBox chk_client;

	@FXML
	private TitledPane pane_server;

	@FXML
	private TitledPane pane_viewer_jfx;

	@FXML
	private TitledPane pane_viewer_cli;

	@FXML
	private TitledPane pane_client;

	@FXML
	private TextField client_key;

	@FXML
	private Button btn_install;

	@FXML
	private Label status;

	@FXML
	private ProgressBar progress;

	/**
	 * The installer to use.
	 */
	private AbstractInstaller installer;

	public MainController() {

		switch (PlatformUtil.queryOsType()) {
		case LINUX:
			installer = new LinuxInstaller(status::setText, progress::setProgress);
			break;
		case MACOS:
			installer = new LinuxInstaller(status::setText, progress::setProgress);
			break;
		case WINDOWS:
			installer = new WindowsInstaller(status::setText, progress::setProgress);
			break;
		default:
			throw new RuntimeException("No installer found");
		}
	}

	@FXML
	private void initialize() {
		chk_server.selectedProperty().addListener(this::refresh);
		chk_viewer_jfx.selectedProperty().addListener(this::refresh);
		chk_viewer_cli.selectedProperty().addListener(this::refresh);
		chk_client.selectedProperty().addListener(this::refresh);

		pane_server.expandedProperty().bind(chk_server.selectedProperty());
		pane_viewer_jfx.expandedProperty().bind(chk_viewer_jfx.selectedProperty());
		pane_viewer_cli.expandedProperty().bind(chk_viewer_cli.selectedProperty());
		pane_client.expandedProperty().bind(chk_client.selectedProperty());
	}

	/**
	 * Refresh the scene's state.
	 */
	private void refresh(ObservableValue<?> p, boolean o, boolean n) {
		// Ensure at least one box is checked
		btn_install.setDisable(!chk_server.isSelected() && !chk_viewer_jfx.isSelected() && !chk_viewer_cli.isSelected()
				&& !chk_client.isSelected());
	}

	@FXML
	private void paste_key() {
		client_key.setText(Clipboard.getSystemClipboard().getString());
	}

	@FXML
	private void install() {

		// Replace buttons with a progressbar
		progress.setVisible(true);
		status.setVisible(true);

		new Thread(new Task<Void>() {

			{
				setOnSucceeded(event -> {
					System.exit(0);
				});

				setOnFailed(event -> {
					exceptionProperty().get().printStackTrace();
					System.exit(0);
				});
			}

			@Override
			public Void call() throws Exception {
				installer.install(chk_server.isSelected(), chk_viewer_jfx.isSelected(), chk_viewer_cli.isSelected());
				return null;
			}

		}).start();
	}
}
