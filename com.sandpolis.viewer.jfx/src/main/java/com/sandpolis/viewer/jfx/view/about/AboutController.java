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
package com.sandpolis.viewer.jfx.view.about;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Date;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.Core;
import com.sandpolis.viewer.jfx.Viewer.UI;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;
import com.sandpolis.viewer.jfx.common.label.DateLabel;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

public class AboutController extends AbstractController {

	@FXML
	private Label version;
	@FXML
	private Label build_time;
	@FXML
	private Label build_platform;
	@FXML
	private Label build_version;
	@FXML
	private Label java_version;
	@FXML
	private Button btn_close;
	@FXML
	private DateLabel java_uptime;
	@FXML
	private BorderPane sub;
	@FXML
	private TableView<ConfigProperty> configuration;

	private double x;
	private double xSpeed = 0.1;
	private double z;
	private double zSpeed = 0;

	@FXML
	private void initialize() throws IOException {

		// Set Sandpolis version
		if (!Core.SO_BUILD.getVersion().isEmpty())
			version.setText(Core.SO_BUILD.getVersion());
		else
			version.setText("?.?.?");

		// Set build timestamp
		build_time.setText(new Date(Core.SO_BUILD.getTime()).toString());

		// Set build platform name
		build_platform.setText(Core.SO_BUILD.getPlatform());

		// Set build Java version
		build_version.setText(Core.SO_BUILD.getJavaVersion());

		// Set current Java version
		java_version.setText(
				String.format("%s (%s)", System.getProperty("java.version"), System.getProperty("java.vendor")));

		// Bind uptime
		java_uptime.referenceProperty().set(ManagementFactory.getRuntimeMXBean().getStartTime());

		// Load configuration data
		for (var entry : Config.entries())
			configuration.getItems().add(new ConfigProperty(entry.getKey(), entry.getValue().toString()));

		if (Platform.isSupported(ConditionalFeature.SCENE3D)) {

			// Load 3D mesh from resource
			MeshView meshView = new MeshView(StlParser.parse(getClass().getResourceAsStream("/mesh/sandpolis.stl")));
			Group bp = new Group(meshView);

			SubScene ss = new SubScene(bp, sub.getWidth(), sub.getHeight(), true, SceneAntialiasing.BALANCED);
			ss.widthProperty().bind(sub.widthProperty());
			ss.heightProperty().bind(sub.heightProperty());
			sub.setCenter(ss);

			// Set position
			meshView.layoutXProperty().bind(Bindings.divide(sub.widthProperty(), 2.0));
			meshView.layoutYProperty().bind(Bindings.divide(sub.heightProperty(), 2.0));

			// Set material
			PhongMaterial sample = new PhongMaterial(Color.rgb(247, 213, 145));
			sample.setSpecularColor(Color.rgb(247, 213, 145));
			sample.setSpecularPower(16);
			meshView.setMaterial(sample);

			KeyFrame kf = new KeyFrame(Duration.millis(2), new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					meshView.getTransforms().setAll(new Rotate(x, Rotate.X_AXIS), new Rotate(z, Rotate.Z_AXIS));
					x += xSpeed;
					z += zSpeed;
				}
			});

			meshView.addEventHandler(MouseEvent.MOUSE_DRAGGED, (MouseEvent event) -> {
				// TODO
				;
			});

			Timeline tl = new Timeline(kf);
			tl.setCycleCount(Animation.INDEFINITE);
			tl.play();
		} else {
			// Just show a static image
			ImageView banner = new ImageView("/image/view/about/banner.png");
			sub.setCenter(banner);
		}

		// Set default button
		Platform.runLater(() -> btn_close.requestFocus());
	}

	@FXML
	private void open_github() {
		UI.getApplication().getHostServices().showDocument("https://github.com/Subterranean-Security/Sandpolis");
	}

	@FXML
	private void open_website() {
		UI.getApplication().getHostServices().showDocument("https://sandpolis.com");
	}

	@FXML
	private void close() {
		sub.getScene().getWindow().hide();
	}

	/**
	 * A container for configuration properties.
	 */
	public static class ConfigProperty {

		private final StringProperty key = new SimpleStringProperty(this, "key");
		private final StringProperty value = new SimpleStringProperty(this, "value");

		public ConfigProperty(String key, String value) {
			this.key.set(key);
			this.value.set(value);
		}

		public StringProperty keyProperty() {
			return key;
		}

		public StringProperty valueProperty() {
			return value;
		}

	}

}
