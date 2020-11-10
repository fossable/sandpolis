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
package com.sandpolis.client.ascetic;

import static com.sandpolis.client.ascetic.store.window.WindowStore.WindowStore;
import static com.sandpolis.core.instance.Environment.printEnvironment;
import static com.sandpolis.core.instance.MainDispatch.register;
import static com.sandpolis.core.instance.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.instance.profile.ProfileStore.ProfileStore;
import static com.sandpolis.core.instance.state.STStore.STStore;
import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.net.network.NetworkStore.NetworkStore;
import static com.sandpolis.core.net.stream.StreamStore.StreamStore;

import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.lanterna.gui2.AsynchronousTextGUIThread;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.SeparateTextGUIThread;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.sandpolis.client.ascetic.view.login.LoginWindow;
import com.sandpolis.core.foundation.Config;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.Task;
import com.sandpolis.core.instance.state.st.ephemeral.EphemeralDocument;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;

/**
 * {@link Client} is responsible for initializing the instance.
 *
 * @since 5.0.0
 */
public final class Client {

	public static final Logger log = LoggerFactory.getLogger(Client.class);

	public static void main(String[] args) {
		printEnvironment(log, "Sandpolis Client");

		register(Client.loadEnvironment);
		register(Client.loadStores);
		register(Client.loadUserInterface);
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

		STStore.init(config -> {
			config.concurrency = 2;
			config.root = new EphemeralDocument();
		});

		ThreadStore.init(config -> {
			config.defaults.put("net.exelet", new NioEventLoopGroup(2));
			config.defaults.put("net.connection.outgoing", new NioEventLoopGroup(2));
			config.defaults.put("net.message.incoming", new UnorderedThreadPoolEventExecutor(2));
			config.defaults.put("store.event_bus", Executors.newSingleThreadExecutor());
		});

		NetworkStore.init(config -> {
		});

		ConnectionStore.init(config -> {
			config.collection = ProfileStore.getByUuid(Core.UUID).get().connection();
		});

		PluginStore.init(config -> {
			config.collection = ProfileStore.getByUuid(Core.UUID).get().plugin();
		});

		WindowStore.init(config -> {
		});

		StreamStore.init(config -> {
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

	private Client() {
	}

	static {
		MainDispatch.register(Client.class);
	}

}
