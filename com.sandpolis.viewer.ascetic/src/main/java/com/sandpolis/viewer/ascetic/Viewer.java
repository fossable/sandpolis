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
package com.sandpolis.viewer.ascetic;

import static com.sandpolis.core.instance.Environment.printEnvironment;
import static com.sandpolis.core.instance.MainDispatch.register;
import static com.sandpolis.core.instance.store.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;

import java.io.IOException;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.sandpolis.core.instance.BasicTasks;
import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.Task;
import com.sandpolis.core.ipc.task.IPCTask;
import com.sandpolis.viewer.ascetic.view.log.LogPanel;
import com.sandpolis.viewer.ascetic.view.login.LoginWindow;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;

/**
 * @author cilki
 * @since 5.0.0
 */
public final class Viewer {

	public static final Logger log = LoggerFactory.getLogger(Viewer.class);

	public static void main(String[] args) {
		printEnvironment(log, "Sandpolis Viewer");

		register(BasicTasks.loadConfiguration);
		register(IPCTask.load);
		register(IPCTask.checkLock);
		register(IPCTask.setLock);
		register(Viewer.loadEnvironment);
		register(Viewer.loadStores);
		register(Viewer.loadUserInterface);
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
			config.defaults.put("store.event_bus", Executors.newSingleThreadExecutor());
		});

		PluginStore.init(config -> {
			config.ephemeral();
		});

		return task.success();
	});

	/**
	 * Load and show the user interface.
	 */
	@InitializationTask(name = "Load user interface")
	private static final Task loadUserInterface = new Task((task) -> {

		new Thread(() -> {
			try {
				Screen screen = new DefaultTerminalFactory().setForceTextTerminal(true).createScreen();
				WindowBasedTextGUI textGUI = new MultiWindowTextGUI(screen);
				screen.startScreen();

				textGUI.addWindow(new LogPanel());
				textGUI.addWindowAndWait(new LoginWindow());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}).start();

		return task.success();
	});

	private Viewer() {
	}

	static {
		MainDispatch.register(Viewer.class);
	}

}
