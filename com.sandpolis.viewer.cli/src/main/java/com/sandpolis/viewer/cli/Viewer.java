/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.viewer.cli;

import static com.sandpolis.core.instance.Environment.EnvPath.LIB;
import static com.sandpolis.core.instance.Environment.EnvPath.LOG;
import static com.sandpolis.core.instance.Environment.EnvPath.TMP;

import java.io.IOException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cilki.slpanels.SLAnimator;
import com.googlecode.lanterna.gui2.AsynchronousTextGUIThread;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.SeparateTextGUIThread;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.sandpolis.core.instance.BasicTasks;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.TaskOutcome;
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

		MainDispatch.register(BasicTasks::loadConfiguration);
		MainDispatch.register(IPCTask::load);
		MainDispatch.register(IPCTask::checkLock);
		MainDispatch.register(IPCTask::setLock);
		MainDispatch.register(Viewer::loadEnvironment);
//		MainDispatch.register(Viewer::loadStores);
//		MainDispatch.register(Viewer::loadPlugins);
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

		if (!Environment.load(TMP, LOG, LIB)) {
			try {
				Environment.setup();
			} catch (RuntimeException e) {
				return task.failure(e);
			}
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

		try {
			Screen screen = new DefaultTerminalFactory().setForceTextTerminal(true).createScreen();
			screen.startScreen();

			WindowBasedTextGUI textGUI = new MultiWindowTextGUI(new SeparateTextGUIThread.Factory(), screen);
			((AsynchronousTextGUIThread) textGUI.getGUIThread()).start();

			// textGUI.addWindow(new LogPanel());
			MainWindow window = new MainWindow();
			textGUI.addWindow(window);
			textGUI.updateScreen();

			// Start the animator. If another Tween-able type is added in addition to
			// MovablePanel, this statement needs to move.
			SLAnimator.start(textGUI.getGUIThread(), window);
		} catch (IOException e) {
			return task.failure(e);
		}

		return task.success();
	}

}
