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
package com.sandpolis.core.ipc.task;

import java.io.IOException;
import java.util.concurrent.Executors;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.ConfigConstant.net;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.TaskOutcome;
import com.sandpolis.core.instance.PoolConstant;
import com.sandpolis.core.instance.store.thread.ThreadStore;
import com.sandpolis.core.ipc.IPCStore;
import com.sandpolis.core.ipc.MCMetadata.RS_Metadata;

/**
 * Contains IPC tasks.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class IPCTask {

	/**
	 * Load the IPC module.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load IPC module", fatal = true)
	public static TaskOutcome load() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		IPCStore.init();

		ThreadStore.register(Executors.newSingleThreadExecutor(), PoolConstant.net.ipc.listener);
		ThreadStore.register(Executors.newSingleThreadExecutor(), PoolConstant.net.ipc.receptor);

		return task.success();
	}

	/**
	 * Check for an existing instance lock. If found, this instance will exit.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Check instance lock", fatal = true)
	public static TaskOutcome checkLock() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		if (!Config.getBoolean(net.ipc.mutex))
			return task.skipped();

		try {
			RS_Metadata metadata = IPCStore.queryInstance(Core.INSTANCE, Core.FLAVOR).orElse(null);
			if (metadata != null) {
				return task.failure("Another instance has been detected (process " + metadata.getPid() + ")");
			}
		} catch (Exception e) {
			return task.failure(e);
		}

		return task.success();
	}

	/**
	 * Set a new instance lock.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Set instance lock", fatal = false)
	public static TaskOutcome setLock() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		try {
			IPCStore.listen(Core.INSTANCE, Core.FLAVOR);
		} catch (IOException e) {
			return task.failure(e);
		}

		return task.success();
	}

	private IPCTask() {
	}
}
