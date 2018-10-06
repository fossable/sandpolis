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
package com.sandpolis.viewer;

import java.io.IOException;
import java.util.Date;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.ShutdownTask;
import com.sandpolis.core.instance.MainDispatch.TaskOutcome;
import com.sandpolis.core.instance.store.pref.PrefStore;
import com.sandpolis.core.ipc.store.IPCStore;
import com.sandpolis.core.proto.ipc.MCMetadata.RS_Metadata;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.util.AsciiUtil;

/**
 * The entry point for Viewer instances. This class is responsible for
 * initializing the new instance and launching the user interface.
 * 
 * @author cilki
 * @since 4.0.0
 */
public final class Viewer {
	private Viewer() {
	}

	public static final Logger log = LoggerFactory.getLogger(Viewer.class);

	private static UI ui;

	public static void main(String[] args) {
		log.info("Launching {} ({})", AsciiUtil.toRainbow("Sandpolis"), Core.SO_BUILD.getVersion());
		log.debug("Built on {} with {} (Build: {})", new Date(Core.SO_BUILD.getTime()), Core.SO_BUILD.getPlatform(),
				Core.SO_BUILD.getNumber());

		MainDispatch.register(Viewer::checkLocks);
		// MainDispatch.register(Viewer::findLocalServer);
		MainDispatch.register(Viewer::loadViewerStores);
		MainDispatch.register(Viewer::loadUserInterface);

		MainDispatch.registerShutdown(Viewer::unloadUserInterface);
	}

	public static void registerUI(UI ui) {
		if (Viewer.ui != null)
			throw new IllegalStateException();
		Viewer.ui = ui;
	}

	public static UI getUI() {
		return ui;
	}

	/**
	 * Check for instance locks. If found, this instance will exit. Otherwise a new
	 * lock is established.
	 * 
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Check instance locks", fatal = true)
	private static TaskOutcome checkLocks() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		if (Config.NO_MUTEX)
			return task.success();

		RS_Metadata metadata = IPCStore.queryInstance(Instance.VIEWER);
		if (metadata != null) {
			return task.failure("Another viewer instance has been detected (process " + metadata.getPid() + ")");
		}

		try {
			IPCStore.listen(Instance.VIEWER);
		} catch (IOException e) {
			log.warn("Failed to initialize an IPC listener", e);
		}

		return task.success();
	}

	/**
	 * Load static stores.
	 * 
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load static stores")
	private static TaskOutcome loadViewerStores() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		// Load PrefStore
		PrefStore.load(Preferences.userRoot());

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

		if (ui == null)
			return task.failure("No user interface found");

		try {
			ui.start();
		} catch (Exception e) {
			return task.failure(e);
		}

		return task.success();
	}

	@ShutdownTask
	private static void unloadUserInterface() {
		try {
			log.debug("Terminating user interface");
			ui.stop();
		} catch (Exception e) {
			log.error("Failed to terminate user interface", e);
		}
	}

}
