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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.sandpolis.core.instance.Store.AutoInitializer;
import com.sandpolis.viewer.jfx.common.FxUtil;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * The {@link StageStore} keeps track of the application's loaded
 * {@link Stage}s.
 * 
 * @author cilki
 * @since 5.0.0
 */
@AutoInitializer
public final class StageStore {

	/**
	 * A list of loaded {@link Stage}s.
	 */
	private static List<Stage> loaded = new ArrayList<>();

	/**
	 * Begin stage building.
	 * 
	 * @return A new {@link StageBuilder}
	 */
	public static StageBuilder newStage() {
		return new StageBuilder();
	}

	/**
	 * Hide all stages in the store.
	 */
	public static void hideAll() {
		loaded.stream().forEach(stage -> stage.hide());
	}

	/**
	 * Show all stages in the store.
	 */
	public static void showAll() {
		loaded.stream().forEach(stage -> stage.show());
	}

	/**
	 * Close a stage.
	 * 
	 * @param stage The stage to close
	 */
	public static void close(Stage stage) {
		loaded.remove(stage);
		stage.close();
	}

	/**
	 * Change the application's global theme.
	 * 
	 * @param theme The new theme
	 */
	public static void changeTheme(String theme) {
		loaded.stream().map(stage -> stage.getScene().getStylesheets()).forEach(styles -> {
			styles.clear();
			styles.add(theme);
		});
	}

	public final static class StageBuilder {

		private Stage stage;
		private Parent root;
		private double width;
		private double height;

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
			if (width == 0)
				width = 100;
			if (height == 0)
				height = 100;

			Scene scene = new Scene(root, width, height);
			scene.getStylesheets().add("/css/Crimson.css");// TODO default theme
			stage.setScene(scene);
			stage.show();
			loaded.add(stage);
		}
	}
}
