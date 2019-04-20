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
package com.sandpolis.viewer.jfx.view.generator;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.proto.net.MCGenerator.RS_Generate;
import com.sandpolis.core.proto.util.Generator.AuthenticationConfig;
import com.sandpolis.core.proto.util.Generator.ExecutionConfig;
import com.sandpolis.core.proto.util.Generator.GenConfig;
import com.sandpolis.core.proto.util.Generator.MegaConfig;
import com.sandpolis.core.proto.util.Generator.MicroConfig;
import com.sandpolis.core.proto.util.Generator.NetworkConfig;
import com.sandpolis.core.proto.util.Generator.NetworkTarget;
import com.sandpolis.core.proto.util.Generator.OutputFormat;
import com.sandpolis.core.proto.util.Generator.OutputPayload;
import com.sandpolis.viewer.cmd.GenCmd;
import com.sandpolis.viewer.jfx.PoolConstant.ui;
import com.sandpolis.viewer.jfx.common.FxUtil;
import com.sandpolis.viewer.jfx.common.controller.FxController;
import com.sandpolis.viewer.jfx.common.pane.ExtendPane;
import com.sandpolis.viewer.jfx.common.pane.ExtendPane.ExtendSide;
import com.sandpolis.viewer.jfx.view.generator.Events.AddServerEvent;
import com.sandpolis.viewer.jfx.view.generator.Events.DetailCloseEvent;
import com.sandpolis.viewer.jfx.view.generator.Events.GenerationCompletedEvent;

import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class GeneratorController extends FxController {

	@FXML
	private ExtendPane extend;
	@FXML
	private TreeView<GenTreeItem> tree;

	private OutputPayload payload;

	private TreeItem<GenTreeItem> exe = new TreeItem<>(new TreeCategory("Execution"));
	private TreeItem<GenTreeItem> net = new TreeItem<>(new TreeCategory("Network").icon("computer.png"));
	private TreeItem<GenTreeItem> auth = new TreeItem<>(new TreeCategory("Authentication").icon("lock.png"));
	private TreeItem<GenTreeItem> plugin = new TreeItem<>(new TreeCategory("Plugins").icon("plugin.png"));
	private TreeItem<GenTreeItem> output = new TreeItem<>(new TreeCategory("Output").icon("compile.png"));

	private TreeAttribute exe_melt = new TreeAttributeList("cleanup installer").options("true", "false").value("false");
	private TreeAttribute exe_recovery = new TreeAttributeList("error recovery").options("true", "false")
			.value("false");
	private TreeAttribute exe_message = new TreeAttributeText("install message");

	private TreeGroup exe_windows = new TreeGroup("Windows");
	private TreeGroup exe_linux = new TreeGroup("Linux");

	private TreeAttribute out_directory = new TreeAttributeText("directory");
	private TreeAttribute out_timestamp = new TreeAttributeText("timestamp");

	private TreeAttribute out_format = new TreeAttributeList("format").options("common/jar_empty.png;jar",
			"file/exe.png;exe", "elf", "file/bat.png;bat", "sh", "rb", "py", "qr", "url");

	private TreeAttribute out_passphrase = new TreeAttributeText("encryption passphrase");

	@FXML
	private void initialize() throws IOException {

		TreeItem<GenTreeItem> root = new TreeItem<>();
		tree.setRoot(root);
		tree.setShowRoot(false);
		tree.setCellFactory(p -> new TreeCell<GenTreeItem>() {
			@Override
			public void updateItem(GenTreeItem item, boolean empty) {
				super.updateItem(item, empty);

				// Unbind properties
				textProperty().unbind();
				graphicProperty().unbind();

				if (empty) {

					// Clear contents
					setText(null);
					setGraphic(null);
					setContextMenu(null);
					return;
				}

				switch (item.getType()) {
				case CATEGORY:
					setText(item.name().get());
					graphicProperty().bind(getItem().icon());
					break;
				case GROUP:
					textProperty().bind(item.name());
					graphicProperty().bind(getItem().icon());
					textFillProperty();// TODO

					ContextMenu menu = new ContextMenu();
					MenuItem menuitem = new MenuItem("Remove");
					menuitem.setOnAction(event -> {
						getTreeItem().getParent().getChildren().remove(getTreeItem());
					});
					menu.getItems().add(menuitem);
					setContextMenu(menu);
					break;
				case ATTRIBUTE:
					TreeAttribute configItem = (TreeAttribute) getItem();
					setGraphic(configItem.getControl());
					break;
				}
			}
		});

		// TODO temporary
		payload = OutputPayload.OUTPUT_MEGA;

		Stream.of(exe, net, auth, plugin, output).forEach(root.getChildren()::add);

		Stream.of(exe_melt, exe_recovery, exe_message, exe_windows, exe_linux).map(TreeItem::new)
				.forEach(exe.getChildren()::add);
		Stream.of(out_format, out_directory, (GenTreeItem) out_timestamp, out_passphrase).map(TreeItem::new)
				.forEach(output.getChildren()::add);

		extend.raise(FxUtil.load("/fxml/view/generator/detail/Status.fxml", this), ExtendSide.BOTTOM, 1000, 100);
	}

	private NetworkConfig getNetworkConfig() {
		net.getChildren().stream().map(n -> {

			return NetworkTarget.newBuilder().setAddress("").build();
		});
		return NetworkConfig.newBuilder().build();
	}

	private ExecutionConfig getExecutionConfig() {
		return ExecutionConfig.newBuilder().setMelt(Boolean.parseBoolean(exe_melt.value().get()))
				.setRecover(Boolean.parseBoolean(exe_recovery.value().get())).build();
	}

	private AuthenticationConfig getAuthenticationConfig() {
		return AuthenticationConfig.newBuilder().build();
	}

	/**
	 * Get a {@link GenConfig} representing the current configuration.
	 * 
	 * @return A new {@link GenConfig}
	 */
	public GenConfig getConfig() {
		var config = GenConfig.newBuilder().setRequestUser("TODO").setPayload(payload).setFormat(OutputFormat.JAR);

		if (payload == OutputPayload.OUTPUT_MEGA)
			config.setMega(MegaConfig.newBuilder().setNetwork(getNetworkConfig()).setExecution(getExecutionConfig())
					.setAuthentication(getAuthenticationConfig()));
		else if (payload == OutputPayload.OUTPUT_MICRO)
			config.setMicro(MicroConfig.newBuilder().setNetwork(getNetworkConfig()).setExecution(getExecutionConfig())
					.setAuthentication(getAuthenticationConfig()));

		return config.build();
	}

	public String searchForProperty(TreeItem<GenTreeItem> item, String target) {
		item.getChildren().stream().map(TreeItem::getValue).filter(i -> i.name().get().equals(target));

		return null;
	}

	@FXML
	private void generate() throws IOException {
		// Collapse all categories
		Stream.of(exe, net, auth, plugin, output).forEach(cat -> cat.setExpanded(false));

		// Raise progress detail
		extend.raise(FxUtil.load("/fxml/view/generator/detail/Progress.fxml", this), ExtendSide.BOTTOM, 1000, 150);

		// Execute command
		GenCmd.async().pool(ui.fx_thread).generate(getConfig()).addListener((ResponseFuture<RS_Generate> response) -> {
			post(GenerationCompletedEvent::new, response.get());
		});
	}

	@FXML
	private void add_network_target() throws IOException {
		TitledPane pane = FxUtil.load("/fxml/view/generator/detail/AddServer.fxml", this);

		extend.raise(pane, ExtendSide.RIGHT, 1000, 300);
	}

	@FXML
	private void add_plugin() throws IOException {
		TitledPane pane = FxUtil.load("/fxml/view/generator/detail/AddPlugin.fxml", this);

		extend.raise(pane, ExtendSide.RIGHT, 1000, 300);
	}

	@FXML
	private void add_group() throws IOException {
		TitledPane pane = FxUtil.load("/fxml/view/generator/detail/AddAuth.fxml", this);

		extend.raise(pane, ExtendSide.RIGHT, 1000, 300);
	}

	@Subscribe
	public void addServer(AddServerEvent event) {
		// Add to tree
		net.getChildren().add(event.get());

		// Select the node and refocus
		tree.getSelectionModel().select(event.get());
		event.get().setExpanded(true);
		tree.requestFocus();
	}

	@Subscribe
	public void closeDetail(DetailCloseEvent event) {
		extend.drop(ExtendSide.RIGHT);
	}

	@Subscribe
	public void setPayload(OutputPayload payload) {
		this.payload = Objects.requireNonNull(payload);
	}
}
