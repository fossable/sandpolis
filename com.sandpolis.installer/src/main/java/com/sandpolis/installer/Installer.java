/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.installer;

import static com.sandpolis.core.instance.MainDispatch.register;
import static com.sandpolis.core.instance.store.pref.PrefStore.PrefStore;
import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;

import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.BasicTasks;
import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.ShutdownTask;
import com.sandpolis.core.instance.MainDispatch.Task;
import com.sandpolis.core.ipc.task.IPCTask;
import com.sandpolis.core.util.AsciiUtil;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * The entry point for Installer instances. This class is responsible for
 * initializing the new instance.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class Installer {

	private static final Logger log = LoggerFactory.getLogger(Installer.class);

	public static void main(String[] args) {
		log.info("Launching {}", AsciiUtil.toRainbow("Sandpolis Installer"));

		register(BasicTasks.loadConfiguration);
		register(Installer.loadConfiguration);
		register(IPCTask.load);
		register(IPCTask.checkLock);
		register(IPCTask.setLock);
		register(Installer.loadStores);
		register(Installer.loadUserInterface);

		register(Installer.shutdown);
	}

	/**
	 * Load instance configuration.
	 */
	@InitializationTask(name = "Load instance configuration")
	private static final Task loadConfiguration = new Task((task) -> {
		Config.register("install.version");
		Config.register("install.path.windows", System.getProperty("user.home") + "/.sandpolis");
		Config.register("install.path.linux", System.getProperty("user.home") + "/.sandpolis");
		Config.register("install.path.macos", System.getProperty("user.home") + "/.sandpolis");

		return task.success();
	});

	/**
	 * Load and show the user interface.
	 */
	@InitializationTask(name = "Load user interface", fatal = true)
	private static final Task loadUserInterface = new Task((task) -> {
		new Thread(() -> Application.launch(UI.class)).start();

		return task.success();
	});

	@InitializationTask(name = "Load stores", fatal = true)
	public static final Task loadStores = new Task((task) -> {

		ThreadStore.init(config -> {
			config.ephemeral();
		});

		PrefStore.init(config -> {
			config.instance = Core.INSTANCE;
			config.flavor = Core.FLAVOR;
		});

		return task.success();
	});

	@ShutdownTask
	public static final Task shutdown = new Task((task) -> {
		ThreadStore.close();
		PrefStore.close();

		return task.success();
	});

	/**
	 * The {@link Application} class for starting the user interface.
	 */
	public static class UI extends Application {

		@Override
		public void start(Stage stage) throws Exception {
			stage.setTitle("Sandpolis Installer");
			stage.setOnCloseRequest(event -> {
				Platform.exit();
				System.exit(0);
			});

			// Set icons
			Stream.of("/image/icon.png", "/image/icon@2x.png", "/image/icon@3x.png", "/image/icon@4x.png")
					.map(UI.class::getResourceAsStream).map(Image::new).forEach(stage.getIcons()::add);

			Parent node = new FXMLLoader(UI.class.getResource("/fxml/Main.fxml")).load();

			Scene scene = new Scene(node, 430, 600);
			scene.getStylesheets().add("/css/default.css");
			stage.setScene(scene);
			stage.setResizable(false);
			stage.show();
		}
	}

	private Installer() {
	}

	static {
		MainDispatch.register(Installer.class);
	}
}
