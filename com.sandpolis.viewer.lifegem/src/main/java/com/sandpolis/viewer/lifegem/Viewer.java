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
package com.sandpolis.viewer.lifegem;

import static com.sandpolis.core.instance.Environment.printEnvironment;
import static com.sandpolis.core.instance.MainDispatch.register;
import static com.sandpolis.core.instance.store.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.instance.store.pref.PrefStore.PrefStore;
import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.store.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.net.store.network.NetworkStore.NetworkStore;
import static com.sandpolis.core.profile.store.ProfileStore.ProfileStore;
import static com.sandpolis.viewer.lifegem.store.stage.StageStore.StageStore;

import java.util.concurrent.Executors;

import com.sandpolis.core.instance.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.ShutdownTask;
import com.sandpolis.core.instance.MainDispatch.Task;
import com.sandpolis.core.ipc.task.IPCTask;
import com.sandpolis.viewer.lifegem.common.FxEventExecutor;
import com.sandpolis.viewer.lifegem.common.FxUtil;

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
		printEnvironment(log, "Sandpolis Viewer");

		register(BasicTasks.loadConfiguration);
		register(IPCTask.load);
		register(IPCTask.checkLock);
		register(IPCTask.setLock);
		register(Viewer.loadEnvironment);
		register(Viewer.loadStores);
		register(Viewer.loadPlugins);
		register(Viewer.loadUserInterface);

		register(Viewer.shutdown);
	}

	/**
	 * Load the runtime environment.
	 */
	@InitializationTask(name = "Load runtime environment", fatal = true)
	private static final Task loadEnvironment = new Task((task) -> {

		Environment.LIB.requireReadable();
		Environment.LOG.set(Config.get("path.log")).requireWritable();
		Environment.PLUGIN.set(Config.get("path.plugin")).requireWritable();
		return task.success();
	});

	/**
	 * Load static stores.
	 */
	@InitializationTask(name = "Load static stores", fatal = true)
	private static final Task loadStores = new Task((task) -> {

		ThreadStore.init(config -> {
			config.ephemeral();
			config.defaults.put("net.exelet", new NioEventLoopGroup(2));
			config.defaults.put("net.connection.outgoing", new NioEventLoopGroup(2));
			config.defaults.put("net.message.incoming", new UnorderedThreadPoolEventExecutor(2));
			config.defaults.put("ui.fx_thread", new FxEventExecutor());
			config.defaults.put("store.event_bus", Executors.newSingleThreadExecutor());
		});

		PrefStore.init(config -> {
			config.instance = Core.INSTANCE;
			config.flavor = Core.FLAVOR;

			config.defaults.put("ui.help", true);
			config.defaults.put("ui.animations", true);
			config.defaults.put("ui.main.view", "list");
			config.defaults.put("ui.main.console", false);
			config.defaults.put("ui.tray.minimize", true);
			config.defaults.put("ui.theme", "Crimson");

			config.defaults.put("ui.view.login.width", 535);
			config.defaults.put("ui.view.login.height", 415);
			config.defaults.put("ui.view.main.width", 770);
			config.defaults.put("ui.view.main.height", 345);
			config.defaults.put("ui.view.about.width", 660);
			config.defaults.put("ui.view.about.height", 400);
			config.defaults.put("ui.view.generator.width", 700);
			config.defaults.put("ui.view.generator.height", 400);
		});

		NetworkStore.init(config -> {
			config.ephemeral();
		});

		ConnectionStore.init(config -> {
			config.ephemeral();
		});

		PluginStore.init(config -> {
			config.ephemeral();
		});

		ProfileStore.init(config -> {
			config.ephemeral(FXCollections.observableArrayList());
		});

		StageStore.init(config -> {
			config.ephemeral();
		});

		return task.success();
	});

	@ShutdownTask
	public static final Task shutdown = new Task((task) -> {
		NetworkStore.close();
		ConnectionStore.close();
		PrefStore.close();
		PluginStore.close();
		ProfileStore.close();
		ThreadStore.close();

		return task.success();
	});

	/**
	 * Load plugins.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load viewer plugins", condition = "plugin.enabled")
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
					.size(PrefStore.getInt("ui.view.login.width"), PrefStore.getInt("ui.view.login.height"))
					.resizable(false).title(FxUtil.translate("stage.login.title")).show();
		}
	}

	private Viewer() {
	}

	static {
		MainDispatch.register(Viewer.class);
	}
}
