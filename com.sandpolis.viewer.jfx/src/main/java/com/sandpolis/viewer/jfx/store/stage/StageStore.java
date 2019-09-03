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
package com.sandpolis.viewer.jfx.store.stage;

import static com.sandpolis.core.instance.store.pref.PrefStore.PrefStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.sandpolis.core.instance.store.StoreBase;
import com.sandpolis.viewer.jfx.PrefConstant.ui;
import com.sandpolis.viewer.jfx.common.FxUtil;
import com.sandpolis.viewer.jfx.store.stage.StageStore.StageStoreConfig;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * The {@link StageStore} keeps track of the application's loaded
 * {@link Stage}s.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class StageStore extends StoreBase<StageStoreConfig> {

	/**
	 * A list of loaded {@link Stage}s.
	 */
	private List<Stage> loaded = new ArrayList<>();

	/**
	 * Begin stage building.
	 * 
	 * @return A new {@link StageBuilder}
	 */
	public StageBuilder newStage() {
		return new StageBuilder();
	}

	/**
	 * Begin stage building.
	 * 
	 * @param stage The initial stage
	 * @return A new {@link StageBuilder}
	 */
	public StageBuilder newStage(Stage stage) {
		return new StageBuilder().stage(stage);
	}

	/**
	 * Hide all stages in the store.
	 */
	public void hideAll() {
		Platform.runLater(() -> {
			loaded.stream().forEach(stage -> stage.hide());
		});
	}

	/**
	 * Show all stages in the store.
	 */
	public void showAll() {
		Platform.runLater(() -> {
			loaded.stream().forEach(stage -> stage.show());
		});
	}

	/**
	 * Close a stage.
	 * 
	 * @param stage The stage to close
	 */
	public void close(Stage stage) {
		loaded.remove(stage);
		Platform.runLater(() -> {
			stage.close();
		});
	}

	/**
	 * Change the application's global theme.
	 * 
	 * @param theme The new theme
	 */
	public void changeTheme(String theme) {
		Objects.requireNonNull(theme);

		PrefStore.putString(ui.theme, theme);
		Platform.runLater(() -> {
			loaded.stream().map(stage -> stage.getScene().getStylesheets()).forEach(styles -> {
				styles.clear();
				styles.add("/css/" + theme + ".css");
			});
		});
	}

	public static final class StageStoreConfig {

	}

	public static final StageStore StageStore = new StageStore();

	public final static class StageBuilder {

		private Stage stage;
		private Parent root;
		private String title;
		private double width;
		private double height;
		private boolean resizable = true;

		private StageBuilder() {
			stage = new Stage();
		}

		/**
		 * Specify a custom {@link Stage}.
		 * 
		 * @param stage The stage
		 * @return {@code this}
		 */
		public StageBuilder stage(Stage stage) {
			if (root != null)
				throw new IllegalStateException("The stage must be specified before the root");

			this.stage = Objects.requireNonNull(stage);
			return this;
		}

		/**
		 * Specify the scene's size.
		 * 
		 * @param width  The scene's width
		 * @param height The scene's height
		 * @return {@code this}
		 */
		public StageBuilder size(double width, double height) {
			this.width = width;
			this.height = height;
			return this;
		}

		/**
		 * Specify the stage's resizable property.
		 * 
		 * @param resizable Whether the stage can resize
		 * @return {@code this}
		 */
		public StageBuilder resizable(boolean resizable) {
			this.resizable = resizable;
			return this;
		}

		/**
		 * Specify the stage's title property.
		 * 
		 * @param title The stage title
		 * @return {@code this}
		 */
		public StageBuilder title(String title) {
			this.title = title;
			return this;
		}

		/**
		 * Load the root of the scene graph.
		 * 
		 * @param root   The root location
		 * @param params Parameters to pass to the controller
		 * @return {@code this}
		 */
		public StageBuilder root(String root, Object... params) {
			// Append stage to end of array
			params = Arrays.copyOf(params, params.length + 1);
			params[params.length - 1] = stage;

			try {
				this.root = FxUtil.loadRoot(Objects.requireNonNull(root), params);
				return this;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * Produce a complete stage and show it on the screen.
		 */
		public void show() {
			Scene scene = new Scene(root, width, height);
			scene.getStylesheets().add("/css/" + PrefStore.getString(ui.theme) + ".css");

			// Set default icons unless they were manually specified
			if (stage.getIcons().size() == 0) {
				Stream.of("/image/icon.png", "/image/icon@2x.png", "/image/icon@3x.png", "/image/icon@4x.png")
						.map(StageStore.class::getResourceAsStream).map(Image::new).forEach(stage.getIcons()::add);
			}

			stage.setScene(scene);
			stage.setResizable(resizable);
			stage.setTitle(title);
			stage.show();

			loaded.add(stage);
		}
	}

	@Override
	public void init(Consumer<StageStoreConfig> o) {
		// TODO Auto-generated method stub

	}
}
