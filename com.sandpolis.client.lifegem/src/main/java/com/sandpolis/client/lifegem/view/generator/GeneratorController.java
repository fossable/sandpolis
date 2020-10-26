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
package com.sandpolis.client.lifegem.view.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.instance.Generator.AuthenticationConfig;
import com.sandpolis.core.instance.Generator.ExecutionConfig;
import com.sandpolis.core.instance.Generator.GenConfig;
import com.sandpolis.core.instance.Generator.LoopConfig;
import com.sandpolis.core.instance.Generator.MegaConfig;
import com.sandpolis.core.instance.Generator.MicroConfig;
import com.sandpolis.core.instance.Generator.NetworkConfig;
import com.sandpolis.core.instance.Generator.NetworkTarget;
import com.sandpolis.core.instance.Generator.OutputFormat;
import com.sandpolis.core.instance.Generator.OutputPayload;
import com.sandpolis.core.clientserver.msg.MsgGenerator.RS_Generate;
import com.sandpolis.core.foundation.util.ValidationUtil;
import com.sandpolis.core.client.cmd.GenCmd;
import com.sandpolis.client.lifegem.common.FxUtil;
import com.sandpolis.client.lifegem.common.controller.AbstractController;
import com.sandpolis.client.lifegem.common.controller.FxController;
import com.sandpolis.client.lifegem.common.pane.ExtendPane;
import com.sandpolis.client.lifegem.common.pane.ExtendPane.ExtendSide;
import com.sandpolis.client.lifegem.view.generator.Events.AddServerEvent;
import com.sandpolis.client.lifegem.view.generator.Events.DetailCloseEvent;
import com.sandpolis.client.lifegem.view.generator.Events.GenerationCompletedEvent;
import com.sandpolis.client.lifegem.view.generator.Events.OutputFormatChangedEvent;
import com.sandpolis.client.lifegem.view.generator.Events.OutputLocationChangedEvent;
import com.sandpolis.client.lifegem.view.generator.config_tree.TreeAttributeFileController;
import com.sandpolis.client.lifegem.view.generator.config_tree.TreeAttributeListController;
import com.sandpolis.client.lifegem.view.generator.config_tree.TreeAttributeTextController;
import com.sandpolis.client.lifegem.view.generator.config_tree.TreeCategoryController;
import com.sandpolis.client.lifegem.view.generator.config_tree.TreeGroupController;
import com.sandpolis.client.lifegem.view.generator.config_tree.TreeItemController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Region;

public class GeneratorController extends FxController {

	@FXML
	private ExtendPane extend;
	@FXML
	private TreeView<Node> tree;
	@FXML
	private Button btn_add_server;
	@FXML
	private Button btn_add_plugin;
	@FXML
	private Button btn_add_group;
	@FXML
	private Button btn_generate;

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
		// Raise status
		Region status = FxUtil.load("/fxml/view/generator/detail/Status.fxml", this);
		status.prefWidthProperty().bind(extend.widthProperty());
		extend.raise(status, ExtendSide.BOTTOM, 1000, 100);

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
		exe.setIcon("/image/common/file-line.svg");

		net = load(this, TreeCategoryController.class);
		net.name().set("Networking");
		net.setIcon("/image/common/global-line.svg");

		auth = load(this, TreeCategoryController.class);
		auth.name().set("Authentication");
		auth.setIcon("/image/common/lock-2-line.svg");

		plugin = load(this, TreeCategoryController.class);
		plugin.name().set("Plugins");
		plugin.setIcon("/image/common/plug-line.svg");

		output = load(this, TreeCategoryController.class);
		output.name().set("Output");
//		output.setIcon("/image/icon16/common/compile.png");

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
		out_directory.value().addListener((p, o, n) -> {
			post(OutputLocationChangedEvent::new, n);
		});
		out_directory.setFileMapper(file -> {
			if (file.isDirectory())
				return file.getAbsolutePath() + "/payload.jar";

			return file.getAbsolutePath();
		});

		out_format = load(output, TreeAttributeListController.class);
		out_format.name().set("File Format");
		out_format.value().addListener((p, o, n) -> {
			post(OutputFormatChangedEvent::new, n);
		});

		out_passphrase = load(output, TreeAttributeTextController.class);
		out_passphrase.name().set("Encryption Passphrase");
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
		return ExecutionConfig.newBuilder().setCleanup(Boolean.parseBoolean(exe_melt.value().get()))
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
	private GenConfig getConfig() {
		var config = GenConfig.newBuilder().setRequestUser("TODO").setPayload(payload).setFormat(OutputFormat.JAR);

		if (payload == OutputPayload.OUTPUT_MEGA)
			config.setMega(MegaConfig.newBuilder().setNetwork(getNetworkConfig()).setExecution(getExecutionConfig())
					.setAuthentication(getAuthenticationConfig()));
		else if (payload == OutputPayload.OUTPUT_MICRO)
			config.setMicro(MicroConfig.newBuilder().setNetwork(getNetworkConfig()).setExecution(getExecutionConfig())
					.setAuthentication(getAuthenticationConfig()));

		return config.build();
	}

	/**
	 * Get the output path.
	 *
	 * @return The current output path
	 */
	private Path getOutput() {
		Path output = Paths.get(out_directory.value().get());
		return output;
	}

	@FXML
	private void generate() throws IOException {
		GenConfig config = getConfig();

		// Collapse all categories
		Stream.of(exe, net, auth, plugin, output).forEach(cat -> {
			cat.getItem().setExpanded(false);
			cat.getItem().getValue().setDisable(true);
		});

		// Disable controls
		btn_add_server.setDisable(true);
		btn_add_plugin.setDisable(true);
		btn_add_group.setDisable(true);
		btn_generate.setDisable(true);

		// Raise progress detail
		Region progress = FxUtil.load("/fxml/view/generator/detail/Progress.fxml", this);
		progress.prefWidthProperty().bind(extend.widthProperty());
		extend.raise(progress, ExtendSide.BOTTOM, 500, 150);

		// Execute
		GenCmd.async().generate(config).thenAccept(rs -> {
			post(GenerationCompletedEvent::new, rs);

			// TODO worker thread
			try {
				Files.write(getOutput(), rs.getOutput().toByteArray());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	@FXML
	private void add_network_target() throws IOException {
		TitledPane pane = FxUtil.load("/fxml/view/generator/detail/AddServer.fxml", this);

		extend.raise(pane, ExtendSide.RIGHT, 800, 300);
	}

	@FXML
	private void add_plugin() throws IOException {
		TitledPane pane = FxUtil.load("/fxml/view/generator/detail/AddPlugin.fxml", this);

		extend.raise(pane, ExtendSide.RIGHT, 800, 300);
	}

	@FXML
	private void add_group() throws IOException {
		TitledPane pane = FxUtil.load("/fxml/view/generator/detail/AddAuth.fxml", this);

		extend.raise(pane, ExtendSide.RIGHT, 800, 300);
	}

	@Subscribe
	public void addServer(AddServerEvent event) throws IOException {

		// Add the server to the configuration tree
		TreeGroupController group = load(net, TreeGroupController.class);
		group.setIcon("/image/common/server-line.png");

		TreeAttributeTextController address = load(group, TreeAttributeTextController.class);
		address.id().set("address");
		address.name().set("address");
		address.setIcon("/image/common/server-line.svg");
		address.validator().set(ValidationUtil::address);
		address.value().set(event.get().address);

		TreeAttributeTextController port = load(group, TreeAttributeTextController.class);
		port.id().set("port");
		port.name().set("port");
//		port.setIcon("/image/icon16/common/ip.png");
		port.validator().set(ValidationUtil::port);
		port.value().set(event.get().port);

		TreeAttributeListController strict_certs = load(group, TreeAttributeListController.class);
		strict_certs.id().set("strict_certs");
		strict_certs.name().set("certificates");
		strict_certs.setIcon("/image/common/file-shield-line.svg");
		strict_certs.getItems().addAll("true", "false");
		strict_certs.value().set(event.get().strict_certs);

		TreeAttributeTextController cooldown = load(group, TreeAttributeTextController.class);
		cooldown.id().set("cooldown");
		cooldown.name().set("cooldown");
//		cooldown.setIcon("/image/icon16/common/ip.png");
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
