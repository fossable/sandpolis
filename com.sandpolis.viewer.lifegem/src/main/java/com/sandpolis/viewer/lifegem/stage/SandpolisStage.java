package com.sandpolis.viewer.lifegem.stage;

import static com.sandpolis.core.instance.pref.PrefStore.PrefStore;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import com.sandpolis.viewer.lifegem.common.FxUtil;

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