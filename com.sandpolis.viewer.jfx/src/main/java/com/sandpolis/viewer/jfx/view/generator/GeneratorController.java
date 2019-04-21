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
import com.sandpolis.core.proto.util.Generator.LoopConfig;
import com.sandpolis.core.proto.util.Generator.MegaConfig;
import com.sandpolis.core.proto.util.Generator.MicroConfig;
import com.sandpolis.core.proto.util.Generator.NetworkConfig;
import com.sandpolis.core.proto.util.Generator.NetworkTarget;
import com.sandpolis.core.proto.util.Generator.OutputFormat;
import com.sandpolis.core.proto.util.Generator.OutputPayload;
import com.sandpolis.core.util.ValidationUtil;
import com.sandpolis.viewer.cmd.GenCmd;
import com.sandpolis.viewer.jfx.PoolConstant.ui;
import com.sandpolis.viewer.jfx.common.FxUtil;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;
import com.sandpolis.viewer.jfx.common.controller.FxController;
import com.sandpolis.viewer.jfx.common.pane.ExtendPane;
import com.sandpolis.viewer.jfx.common.pane.ExtendPane.ExtendSide;
import com.sandpolis.viewer.jfx.view.generator.Events.AddServerEvent;
import com.sandpolis.viewer.jfx.view.generator.Events.DetailCloseEvent;
import com.sandpolis.viewer.jfx.view.generator.Events.GenerationCompletedEvent;
import com.sandpolis.viewer.jfx.view.generator.config_tree.TreeAttributeFileController;
import com.sandpolis.viewer.jfx.view.generator.config_tree.TreeAttributeListController;
import com.sandpolis.viewer.jfx.view.generator.config_tree.TreeAttributeTextController;
import com.sandpolis.viewer.jfx.view.generator.config_tree.TreeCategoryController;
import com.sandpolis.viewer.jfx.view.generator.config_tree.TreeGroupController;
import com.sandpolis.viewer.jfx.view.generator.config_tree.TreeItemController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class GeneratorController extends FxController {

	@FXML
	private ExtendPane extend;
	@FXML
	private TreeView<Node> tree;

	private TreeCategoryController exe;
	private TreeCategoryController net;
	private TreeCategoryController auth;
	private TreeCategoryController plugin;
	private TreeCategoryController output;

	private TreeAttributeListController exe_melt;
	private TreeAttributeListController exe_recovery;
	private TreeAttributeTextController exe_message;

	private TreeGroupController exe_windows;
	private TreeGroupController exe_linux;

	private TreeAttributeFileController out_directory;
	private TreeAttributeListController out_format;
	private TreeAttributeTextController out_passphrase;

	private OutputPayload payload;

	private <C extends TreeItemController> C load(AbstractController parent, Class<?> type) throws IOException {
		if (!type.getSimpleName().endsWith("Controller"))
			throw new IllegalArgumentException();

		FXMLLoader loader = new FXMLLoader(GeneratorController.class.getResource(
				"/fxml/view/generator/config_tree/" + type.getSimpleName().replace("Controller", "") + ".fxml"));

		TreeItem<Node> item = new TreeItem<>(loader.load());
		C controller = loader.getController();
		controller.setItem(item);

		if (parent == this)
			tree.getRoot().getChildren().add(item);
		if (parent instanceof TreeItemController)
			((TreeItemController) parent).getItem().getChildren().add(item);
		if (parent instanceof TreeGroupController)
			((TreeGroupController) parent).getChildren().add(controller);
		return controller;
	}

	@FXML
	private void initialize() throws IOException {
		tree.setRoot(new TreeItem<>());
		tree.setShowRoot(false);
		tree.setCellFactory(p -> new TreeCell<Node>() {
			@Override
			public void updateItem(Node item, boolean empty) {
				super.updateItem(item, empty);

				// Unbind properties
				textProperty().unbind();
				graphicProperty().unbind();

				if (empty) {
					setText(null);
					setGraphic(null);
					setContextMenu(null);
				} else {
					setGraphic(item);
				}
			}
		});

		exe = load(this, TreeCategoryController.class);
		exe.name().set("Execution");
		exe.setIcon("/image/icon16/common/computer.png");

		net = load(this, TreeCategoryController.class);
		net.name().set("Networking");
		net.setIcon("/image/icon16/common/computer.png");

		auth = load(this, TreeCategoryController.class);
		auth.name().set("Authentication");
		auth.setIcon("/image/icon16/common/lock.png");

		plugin = load(this, TreeCategoryController.class);
		plugin.name().set("Plugins");
		plugin.setIcon("/image/icon16/common/plugin.png");

		output = load(this, TreeCategoryController.class);
		output.name().set("Output");
		output.setIcon("/image/icon16/common/compile.png");

		exe_melt = load(exe, TreeAttributeListController.class);
		exe_melt.name().set("Delete installer when done");
		exe_melt.getItems().addAll("true", "false");
		exe_melt.value().set("false");

		exe_recovery = load(exe, TreeAttributeListController.class);
		exe_recovery.name().set("Recover from errors");

		exe_message = load(exe, TreeAttributeTextController.class);
		exe_message.name().set("Installation message");

		exe_windows = load(exe, TreeGroupController.class);
		exe_windows.name().set("Windows");

		exe_linux = load(exe, TreeGroupController.class);
		exe_linux.name().set("Linux");

		out_directory = load(output, TreeAttributeFileController.class);
		out_directory.name().set("Output Location");
		out_directory.value().set(System.getProperty("user.home"));

		out_format = load(output, TreeAttributeListController.class);
		out_format.name().set("File Format");

		out_passphrase = load(output, TreeAttributeTextController.class);
		out_passphrase.name().set("Encryption Passphrase");

		// Raise status
		extend.raise(FxUtil.load("/fxml/view/generator/detail/Status.fxml", this), ExtendSide.BOTTOM, 1000, 100);
	}

	private NetworkConfig getNetworkConfig() {
		LoopConfig.Builder config = LoopConfig.newBuilder();

		for (TreeItemController c : net.getChildren()) {
			if (c instanceof TreeGroupController) {
				TreeGroupController group = (TreeGroupController) c;
				config.addTarget(NetworkTarget.newBuilder().setAddress(group.getValueForId("address").get())
						.setPort(Integer.parseInt(group.getValueForId("port").get())));
			}
		}

		return NetworkConfig.newBuilder().setLoopConfig(config).build();
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

	@FXML
	private void generate() throws IOException {
		// Collapse all categories
		Stream.of(exe, net, auth, plugin, output).forEach(cat -> cat.getItem().setExpanded(false));

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
	public void addServer(AddServerEvent event) throws IOException {

		// Add the server to the configuration tree
		TreeGroupController group = load(net, TreeGroupController.class);
		group.setIcon("/image/icon16/common/server.png");

		TreeAttributeTextController address = load(group, TreeAttributeTextController.class);
		address.id().set("address");
		address.name().set("address");
		address.setIcon("/image/icon16/common/ip.png");
		address.validator().set(ValidationUtil::address);
		address.value().set(event.get().address);

		TreeAttributeTextController port = load(group, TreeAttributeTextController.class);
		port.id().set("port");
		port.name().set("port");
		port.setIcon("/image/icon16/common/ip.png");
		port.validator().set(ValidationUtil::port);
		port.value().set(event.get().port);

		TreeAttributeListController strict_certs = load(group, TreeAttributeListController.class);
		strict_certs.id().set("strict_certs");
		strict_certs.name().set("certificates");
		strict_certs.setIcon("/image/icon16/common/ssl_certificate.png");
		strict_certs.getItems().addAll("true", "false");
		strict_certs.value().set(event.get().strict_certs);

		TreeAttributeTextController cooldown = load(group, TreeAttributeTextController.class);
		cooldown.id().set("cooldown");
		cooldown.name().set("cooldown");
		cooldown.setIcon("/image/icon16/common/ip.png");
		cooldown.value().set(event.get().cooldown);

		// Bind address to group name
		group.name().bind(address.value());

		// Select the node and refocus
		tree.getSelectionModel().select(group.getItem());
		group.getItem().setExpanded(true);
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
