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

import java.util.Date;
import java.util.prefs.BackingStoreException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.attribute.AttributeKey;
import com.sandpolis.core.instance.BasicTasks;
import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.ConfigConstant.plugin;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.TaskOutcome;
import com.sandpolis.core.instance.PoolConstant.net;
import com.sandpolis.core.instance.storage.MemoryListStoreProvider;
import com.sandpolis.core.instance.store.plugin.Plugin;
import com.sandpolis.core.instance.store.plugin.PluginStore;
import com.sandpolis.core.instance.store.pref.PrefStore;
import com.sandpolis.core.instance.store.thread.ThreadStore;
import com.sandpolis.core.ipc.IPCTasks;
import com.sandpolis.core.net.store.network.NetworkStore;
import com.sandpolis.core.profile.ProfileStore;
import com.sandpolis.core.util.AsciiUtil;
import com.sandpolis.viewer.jfx.PrefConstant.ui;
import com.sandpolis.viewer.jfx.attribute.ObservableAttribute;
import com.sandpolis.viewer.jfx.common.FxEventExecutor;
import com.sandpolis.viewer.jfx.store.stage.StageStore;

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

		MainDispatch.register(BasicTasks::loadConfiguration);
		MainDispatch.register(Viewer::loadConfiguration);
		MainDispatch.register(IPCTasks::checkLocks);
		MainDispatch.register(Viewer::loadEnvironment);
		MainDispatch.register(BasicTasks::loadStores);
		MainDispatch.register(Viewer::loadStores);
		MainDispatch.register(Viewer::loadPlugins);
		MainDispatch.register(Viewer::loadUserInterface);
	}

	/**
	 * Load the configuration from the runtime environment.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load viewer configuration", fatal = true)
	private static TaskOutcome loadConfiguration() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		// Load PrefStore
		PrefStore.load(Viewer.class);

		try {
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
		} catch (BackingStoreException e) {
			return task.failure(e);
		}

		return task.success();
	}

	/**
	 * Load the runtime environment.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load runtime environment", fatal = true)
	private static TaskOutcome loadEnvironment() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		if (!Environment.load(LOG, TMP, LIB)) {
			try {
				Environment.setup();
			} catch (RuntimeException e) {
				return task.failure(e);
			}
		}

		return task.success();
	}

	/**
	 * Load static stores.
	 * 
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load static stores", fatal = true)
	public static TaskOutcome loadStores() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		// Load ThreadStore
		ThreadStore.register(new NioEventLoopGroup(2), net.exelet);
		ThreadStore.register(new NioEventLoopGroup(2), net.connection.outgoing);
		ThreadStore.register(new UnorderedThreadPoolEventExecutor(2), net.message.incoming);
		ThreadStore.register(new FxEventExecutor(), PoolConstant.ui.fx_thread);

		// Load NetworkStore
		NetworkStore.init();

		// Load PluginStore
		PluginStore.init(new MemoryListStoreProvider<Plugin>(Plugin.class));

		// Load ProfileStore
		ProfileStore.load(FXCollections.observableArrayList());

		// Configure attributekeys
		AttributeKey.setDefaultAttributeClass(ObservableAttribute.class);

		return task.success();
	}

	/**
	 * Load plugins.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load viewer plugins")
	private static TaskOutcome loadPlugins() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		if (Config.getBoolean(plugin.enabled))
			return task.skipped();

		try {
			PluginStore.scanPluginDirectory();

			PluginStore.loadPlugins();
		} catch (Exception e) {
			return task.failure(e);
		}

		return task.success();
	}

	/**
	 * Load and show the user interface.
	 * 
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load user interface")
	private static TaskOutcome loadUserInterface() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		new Thread(() -> Application.launch(UI.class)).start();

		return task.success();
	}

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
					.resizable(false).show();
		}
	}
}
