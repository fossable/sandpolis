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
package com.sandpolis.client.lifegem.stage;

import static com.sandpolis.core.instance.pref.PrefStore.PrefStore;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import com.sandpolis.client.lifegem.common.FxUtil;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class SandpolisStage extends Stage {

	public SandpolisStage() {
		Stream.of("/image/icon.png", "/image/icon@2x.png", "/image/icon@3x.png", "/image/icon@4x.png")
				.map(StageStore.class::getResourceAsStream).map(Image::new).forEach(getIcons()::add);

	}

	/**
	 * Load the root of the scene graph.
	 *
	 * @param root   The root location
	 * @param params Parameters to pass to the controller
	 * @return {@code this}
	 */
	public void setRoot(String root, Object... params) {
		// Append stage to end of array
		params = Arrays.copyOf(params, params.length + 1);
		params[params.length - 1] = this;

		try {
			setScene(new Scene(FxUtil.loadRoot(Objects.requireNonNull(root), params)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		getScene().getStylesheets().add("/css/" + PrefStore.getString("ui.theme") + ".css");
	}

}
