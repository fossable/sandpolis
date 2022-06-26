//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.stage;

import static org.s7s.core.instance.pref.PrefStore.PrefStore;

import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import tornadofx.FX;
import tornadofx.View;

public class SandpolisStage extends Stage {

	private static final Logger log = LoggerFactory.getLogger(SandpolisStage.class);

	public SandpolisStage() {
		Stream.of("/image/icon.png", "/image/icon@2x.png", "/image/icon@3x.png", "/image/icon@4x.png")
				.map(StageStore.class::getResourceAsStream).map(Image::new).forEach(getIcons()::add);

	}

	public void setRoot(String root, Object... params) {
		try {
			setRoot((Class<View>) Class.forName(root), params);
		} catch (Exception e) {
			log.error("Failed to load scene root", e);
		}
	}

	/**
	 * Load the root of the scene graph.
	 */
	public void setRoot(Class<View> root, Object... params) {

		try {
			var scene = new Scene(FX.find(root).getRoot());
			scene.getStylesheets().add("/css/" + PrefStore.getString("ui.theme") + ".css");
			setScene(scene);
		} catch (Exception e) {
			log.error("Failed to load scene root", e);
		}
	}

}
