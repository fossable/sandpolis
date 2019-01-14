/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.viewer.jfx.view.about;

import java.util.Date;

import com.interactivemesh.jfx.importer.stl.StlMeshImporter;
import com.sandpolis.core.instance.Core;
import com.sandpolis.viewer.jfx.Viewer.UI;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

public class AboutController {

	@FXML
	private Label version;
	@FXML
	private Label build_number;
	@FXML
	private Label build_time;
	@FXML
	private Label build_platform;
	@FXML
	private Label java_version;
	@FXML
	private Label java_uptime;
	@FXML
	private Pane sub;

	private double x;
	private double xSpeed = 0.1;
	private double z;
	private double zSpeed = 0;

	@FXML
	private void initialize() {

		// Load static properties
		version.setText(Core.SO_BUILD.getVersion());
		build_number.setText(Integer.toString(Core.SO_BUILD.getNumber()));
		build_time.setText(new Date(Core.SO_BUILD.getTime()).toString());
		build_platform.setText(Core.SO_BUILD.getPlatform());
		java_version.setText(Core.SO_BUILD.getJavaVersion());

		// Load 3D mesh from resource
		StlMeshImporter importer = new StlMeshImporter();
		importer.read(getClass().getResource("/mesh/sandpolis.stl"));
		MeshView meshView = new MeshView(importer.getImport());
		Group bp = new Group(meshView);

		SubScene ss = new SubScene(bp, sub.getWidth(), sub.getHeight(), true, SceneAntialiasing.BALANCED);
		ss.widthProperty().bind(sub.widthProperty());
		ss.heightProperty().bind(sub.heightProperty());
		sub.getChildren().add(ss);

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

}
