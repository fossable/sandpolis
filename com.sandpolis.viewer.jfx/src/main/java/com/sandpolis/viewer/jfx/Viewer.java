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

import static com.sandpolis.core.instance.Environment.EnvPath.DB;
import static com.sandpolis.core.instance.Environment.EnvPath.JLIB;
import static com.sandpolis.core.instance.Environment.EnvPath.LOG;
import static com.sandpolis.core.instance.Environment.EnvPath.NLIB;
import static com.sandpolis.core.instance.Environment.EnvPath.TMP;

import java.util.Date;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.BasicTasks;
import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.TaskOutcome;
import com.sandpolis.core.instance.Signaler;
import com.sandpolis.core.instance.storage.MemoryListStoreProvider;
import com.sandpolis.core.instance.store.plugin.Plugin;
import com.sandpolis.core.instance.store.plugin.PluginStore;
import com.sandpolis.core.instance.store.thread.ThreadStore;
import com.sandpolis.core.ipc.IPCTasks;
import com.sandpolis.core.net.store.network.NetworkStore;
import com.sandpolis.core.util.AsciiUtil;
import com.sandpolis.viewer.jfx.common.FxEventExecutor;
import com.sandpolis.viewer.jfx.view.login.LoginController;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
		MainDispatch.register(IPCTasks::checkLocks);
		MainDispatch.register(Viewer::loadEnvironment);
		MainDispatch.register(Viewer::loadStores);
		MainDispatch.register(Viewer::loadPlugins);
		MainDispatch.register(Viewer::loadUserInterface);
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

		if (!Environment.load(DB, TMP, LOG, JLIB, NLIB)) {
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
		ThreadStore.register(Executors.newSingleThreadExecutor(r -> {
			var s = new Thread(r, "SIGNALER");
			s.setDaemon(true);
			return s;
		}), "signaler");
		ThreadStore.register(new NioEventLoopGroup(4), "net.exelet");
		ThreadStore.register(new NioEventLoopGroup(2), "net.connection.outgoing");
		ThreadStore.register(new UnorderedThreadPoolEventExecutor(2), "net.message.incoming");
		ThreadStore.register(new FxEventExecutor(), "ui.fx");
		Signaler.init(ThreadStore.get("signaler"));

		// Load NetworkStore
		NetworkStore.init();

		// Load PluginStore
		PluginStore.init(new MemoryListStoreProvider<Plugin>(Plugin.class));

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

		if (Config.getBoolean("no_plugins"))
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

		new Thread(() -> Application.launch(new Application() {
			@Override
			public void start(Stage stage) throws Exception {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/view/login/Login.fxml"));
				Parent root = loader.load();
				((LoginController) loader.getController()).setStage(stage);
				Scene scene = new Scene(root, 420, 380);
				stage.setScene(scene);
				stage.show();
			}
		}.getClass())).start();

		return task.success();
	}

}
