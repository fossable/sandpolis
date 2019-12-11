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
package com.sandpolis.viewer.cli;

import static com.sandpolis.core.instance.MainDispatch.register;

import java.util.Date;

import com.sandpolis.core.instance.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.lanterna.gui2.AsynchronousTextGUIThread;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.SeparateTextGUIThread;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.Task;
import com.sandpolis.core.ipc.task.IPCTask;
import com.sandpolis.core.util.AsciiUtil;
import com.sandpolis.viewer.cli.view.main.MainWindow;

/**
 * @author cilki
 * @since 5.0.0
 */
public final class Viewer {

	public static final Logger log = LoggerFactory.getLogger(Viewer.class);

	public static void main(String[] args) {
		log.info("Launching {} ({})", AsciiUtil.toRainbow("Sandpolis Viewer"), Core.SO_BUILD.getVersion());
		log.debug("Built on {} with {} (Build: {})", new Date(Core.SO_BUILD.getTime()), Core.SO_BUILD.getPlatform(),
				Core.SO_BUILD.getNumber());

		register(BasicTasks.loadConfiguration);
		register(IPCTask.load);
		register(IPCTask.checkLock);
		register(IPCTask.setLock);
		register(Viewer.loadEnvironment);
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
	 * Load and show the user interface.
	 */
	@InitializationTask(name = "Load user interface")
	private static final Task loadUserInterface = new Task((task) -> {
		Screen screen = new DefaultTerminalFactory().setForceTextTerminal(true).createScreen();
		screen.startScreen();

		WindowBasedTextGUI textGUI = new MultiWindowTextGUI(new SeparateTextGUIThread.Factory(), screen);
		((AsynchronousTextGUIThread) textGUI.getGUIThread()).start();

		// textGUI.addWindow(new LogPanel());
		MainWindow window = new MainWindow();
		textGUI.addWindow(window);
		textGUI.updateScreen();

		return task.success();
	});

	private Viewer() {
	}

	static {
		MainDispatch.register(Viewer.class);
	}

}
