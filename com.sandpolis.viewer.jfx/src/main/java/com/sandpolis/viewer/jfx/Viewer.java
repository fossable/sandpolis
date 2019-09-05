/******************************************************************************
 *                                                                            *
 *                    Copyright 2016 Subterranean Security                    *
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
package com.sandpolis.viewer.jfx;

import static com.sandpolis.core.instance.Environment.EnvPath.LIB;
import static com.sandpolis.core.instance.Environment.EnvPath.LOG;
import static com.sandpolis.core.instance.Environment.EnvPath.TMP;
import static com.sandpolis.core.instance.store.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.instance.store.pref.PrefStore.PrefStore;
import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.store.network.NetworkStore.NetworkStore;
import static com.sandpolis.core.profile.ProfileStore.ProfileStore;
import static com.sandpolis.viewer.jfx.store.stage.StageStore.StageStore;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.attribute.AttributeKey;
import com.sandpolis.core.instance.BasicTasks;
import com.sandpolis.core.instance.ConfigConstant.plugin;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.Task;
import com.sandpolis.core.instance.PoolConstant.net;
import com.sandpolis.core.ipc.task.IPCTask;
import com.sandpolis.core.util.AsciiUtil;
import com.sandpolis.viewer.jfx.PrefConstant.ui;
import com.sandpolis.viewer.jfx.attribute.ObservableAttribute;
import com.sandpolis.viewer.jfx.common.FxEventExecutor;
import com.sandpolis.viewer.jfx.common.FxUtil;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.stage.Stage;

/**
 * @author cilki
 * @since 4.0.0
 */
public final class Viewer {

	private static final Logger log = LoggerFactory.getLogger(Viewer.class);

	public static void main(String[] args) {
		log.info("Launching {} ({})", AsciiUtil.toRainbow("Sandpolis Viewer"), Core.SO_BUILD.getVersion());
		log.debug("Built on {} with {} (Build: {})", new Date(Core.SO_BUILD.getTime()), Core.SO_BUILD.getPlatform(),
				Core.SO_BUILD.getNumber());

		MainDispatch.register(BasicTasks.loadConfiguration);
		MainDispatch.register(Viewer.loadConfiguration);
		MainDispatch.register(IPCTask.load);
		MainDispatch.register(IPCTask.checkLock);
		MainDispatch.register(IPCTask.setLock);
		MainDispatch.register(Viewer.loadEnvironment);
		MainDispatch.register(Viewer.loadStores);
		MainDispatch.register(Viewer.loadPlugins);
		MainDispatch.register(Viewer.loadUserInterface);
	}

	/**
	 * Load the configuration from the runtime environment.
	 */
	@InitializationTask(name = "Load viewer configuration", fatal = true)
	private static final Task loadConfiguration = new Task((task) -> {

		// Load PrefStore
		PrefStore.init(config -> {
			config.instance = Core.INSTANCE;
			config.flavor = Core.FLAVOR;
		});

		PrefStore.register(ui.help, true);
		PrefStore.register(ui.animations, true);
		PrefStore.register(ui.main.view, "list");
		PrefStore.register(ui.main.console, false);
		PrefStore.register(ui.tray.minimize, true);
		PrefStore.register(ui.theme, "Crimson");

		PrefStore.register(ui.view.login.width, 535);
		PrefStore.register(ui.view.login.height, 380);
		PrefStore.register(ui.view.main.width, 770);
		PrefStore.register(ui.view.main.height, 345);
		PrefStore.register(ui.view.about.width, 660);
		PrefStore.register(ui.view.about.height, 400);
		PrefStore.register(ui.view.generator.width, 700);
		PrefStore.register(ui.view.generator.height, 400);

		return task.success();
	});

	/**
	 * Load the runtime environment.
	 */
	@InitializationTask(name = "Load runtime environment", fatal = true)
	private static final Task loadEnvironment = new Task((task) -> {

		if (!Environment.load(LOG, TMP, LIB))
			Environment.setup();

		return task.success();
	});

	/**
	 * Load static stores.
	 */
	@InitializationTask(name = "Load static stores", fatal = true)
	private static final Task loadStores = new Task((task) -> {

		ThreadStore.init(config -> {
			config.ephemeral();
			config.register(new NioEventLoopGroup(2), net.exelet);
			config.register(new NioEventLoopGroup(2), net.connection.outgoing);
			config.register(new UnorderedThreadPoolEventExecutor(2), net.message.incoming);
			config.register(new FxEventExecutor(), PoolConstant.ui.fx_thread);
		});

		NetworkStore.init(config -> {
			config.ephemeral();
		});

		PluginStore.init(config -> {
			config.ephemeral();
		});

		ProfileStore.init(config -> {
			// TODO
			FXCollections.observableArrayList();
		});

		// TODO
		AttributeKey.setDefaultAttributeClass(ObservableAttribute.class);

		return task.success();
	});

	/**
	 * Load plugins.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load viewer plugins", condition = plugin.enabled)
	private static final Task loadPlugins = new Task((task) -> {
		PluginStore.scanPluginDirectory();
		PluginStore.loadPlugins();

		return task.success();
	});

	/**
	 * Load and show the user interface.
	 * 
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load user interface")
	private static final Task loadUserInterface = new Task((task) -> {
		new Thread(() -> Application.launch(UI.class)).start();

		return task.success();
	});

	/**
	 * The {@link Application} class for starting the user interface.
	 */
	public static class UI extends Application {

		private static Application singleton;

		public UI() {
			if (singleton != null)
				throw new IllegalStateException();

			singleton = this;
		}

		/**
		 * Get the application handle.
		 * 
		 * @return The {@link Application} or {@code null} if it has not started
		 */
		public static Application getApplication() {
			return singleton;
		}

		@Override
		public void start(Stage stage) throws Exception {
			StageStore.newStage().stage(stage).root("/fxml/view/login/Login.fxml")
					.size(PrefStore.getInt(ui.view.login.width), PrefStore.getInt(ui.view.login.height))
					.resizable(false).title(FxUtil.translate("stage.login.title")).show();
		}
	}

	private Viewer() {
	}

	static {
		MainDispatch.register(Viewer.class);
	}
}
