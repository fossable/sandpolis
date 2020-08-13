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
import static com.sandpolis.core.instance.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.net.network.NetworkStore.NetworkStore;
import static com.sandpolis.core.net.stream.StreamStore.StreamStore;
import static com.sandpolis.viewer.ascetic.store.window.WindowStore.WindowStore;

import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.lanterna.gui2.AsynchronousTextGUIThread;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.SeparateTextGUIThread;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.sandpolis.core.foundation.Config;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.Task;
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

		register(Viewer.loadEnvironment);
		register(Viewer.loadStores);
		register(Viewer.loadUserInterface);
	}

	/**
	 * Load the runtime environment.
	 */
	@InitializationTask(name = "Load runtime environment", fatal = true)
	private static final Task loadEnvironment = new Task(outcome -> {

		Environment.LIB.set(Config.PATH_LIB.value().orElse(null)).requireReadable();
		Environment.LOG.set(Config.PATH_LOG.value().orElse(null)).requireWritable();
		Environment.PLUGIN.set(Config.PATH_PLUGIN.value().orElse(null)).requireWritable();
		return outcome.success();
	});

	/**
	 * Load static stores.
	 */
	@InitializationTask(name = "Load static stores", fatal = true)
	private static final Task loadStores = new Task(outcome -> {

		ThreadStore.init(config -> {
			config.ephemeral();
			config.defaults.put("net.exelet", new NioEventLoopGroup(2));
			config.defaults.put("net.connection.outgoing", new NioEventLoopGroup(2));
			config.defaults.put("net.message.incoming", new UnorderedThreadPoolEventExecutor(2));
			config.defaults.put("store.event_bus", Executors.newSingleThreadExecutor());
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

		WindowStore.init(config -> {
			config.ephemeral();
		});

		StreamStore.init(config -> {
			config.ephemeral();
		});

		return outcome.success();
	});

	/**
	 * Load and show the user interface.
	 */
	@InitializationTask(name = "Load user interface")
	private static final Task loadUserInterface = new Task(outcome -> {

		TerminalScreen screen = new DefaultTerminalFactory().setForceTextTerminal(true).createScreen();
		WindowBasedTextGUI textGUI = new MultiWindowTextGUI(new SeparateTextGUIThread.Factory(), screen);
		((AsynchronousTextGUIThread) textGUI.getGUIThread()).start();
		screen.startScreen();
		WindowStore.gui = textGUI;

		WindowStore.create(LoginWindow::new);

		return outcome.success();
	});

	private Viewer() {
	}

	static {
		MainDispatch.register(Viewer.class);
	}

}
