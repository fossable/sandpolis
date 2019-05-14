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

import java.util.function.Consumer;
import java.util.stream.Stream;

import com.sandpolis.core.instance.PlatformUtil;
import com.sandpolis.installer.install.AbstractInstaller;
import com.sandpolis.installer.install.LinuxInstaller;
import com.sandpolis.installer.install.WindowsInstaller;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

	@FXML
	private ImageView banner;

	/**
	 * The installer to use.
	 */
	private AbstractInstaller installer;

	@FXML
	private void initialize() {
		Consumer<String> status = (s) -> {
			Platform.runLater(() -> this.status.setText(s));
		};

		Consumer<Double> progress = (p) -> {
			Platform.runLater(() -> this.progress.setProgress(p));
		};

		switch (PlatformUtil.queryOsType()) {
		case LINUX:
			installer = new LinuxInstaller(status, progress);
			break;
		case MACOS:
			installer = new LinuxInstaller(status, progress);
			break;
		case WINDOWS:
			installer = new WindowsInstaller(status, progress);
			break;
		default:
			throw new RuntimeException("No installer found");
		}

		chk_server.selectedProperty().addListener(this::refresh);
		chk_viewer_jfx.selectedProperty().addListener(this::refresh);
		chk_viewer_cli.selectedProperty().addListener(this::refresh);
		chk_client.selectedProperty().addListener(this::refresh);

		pane_server.expandedProperty().bindBidirectional(chk_server.selectedProperty());
		pane_viewer_jfx.expandedProperty().bindBidirectional(chk_viewer_jfx.selectedProperty());
		pane_viewer_cli.expandedProperty().bindBidirectional(chk_viewer_cli.selectedProperty());
		pane_client.expandedProperty().bindBidirectional(chk_client.selectedProperty());

		banner.setImage(new Image(MainController.class.getResourceAsStream("/image/logo.png")));
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
				});

				setOnFailed(event -> {
					btn_install.setVisible(false);
					progress.setVisible(false);
					status.setText("Installation failed!");

					exceptionProperty().get().printStackTrace();
				});
			}

			@Override
			public Void call() throws Exception {
				double progressIncrement = 100 / Stream.of(chk_server, chk_viewer_jfx, chk_viewer_cli, chk_client)
						.filter(c -> c.isSelected()).count();

				if (chk_server.isSelected())
					installer.installServer(progressIncrement);
				if (chk_viewer_jfx.isSelected())
					installer.installViewerJfx(progressIncrement);
				if (chk_viewer_cli.isSelected())
					installer.installViewerCli(progressIncrement);
				if (chk_client.isSelected())
					installer.installClient(progressIncrement, client_key.getText());

				return null;
			}

		}).start();
	}
}
