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

import java.util.concurrent.Executors;

import com.sandpolis.core.instance.ConfigConstant.net;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.Task;
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
	 */
	@InitializationTask(name = "Load IPC module", condition = net.ipc.mutex, fatal = true)
	public static final Task load = new Task((task) -> {
		IPCStore.init();

		ThreadStore.register(Executors.newSingleThreadExecutor(), PoolConstant.net.ipc.listener);
		ThreadStore.register(Executors.newSingleThreadExecutor(), PoolConstant.net.ipc.receptor);

		return task.success();
	});

	/**
	 * Check for an existing instance lock. If found, this instance will exit.
	 */
	@InitializationTask(name = "Check instance lock", condition = net.ipc.mutex, fatal = true)
	public static final Task checkLock = new Task((task) -> {

		RS_Metadata metadata = IPCStore.queryInstance(Core.INSTANCE, Core.FLAVOR).orElse(null);
		if (metadata != null)
			return task.failure("Another instance has been detected (process " + metadata.getPid() + ")");

		return task.success();
	});

	/**
	 * Set a new instance lock.
	 */
	@InitializationTask(name = "Set instance lock", condition = net.ipc.mutex, fatal = false)
	public static final Task setLock = new Task((task) -> {
		IPCStore.listen(Core.INSTANCE, Core.FLAVOR);

		return task.success();
	});

	private IPCTask() {
	}

	static {
		MainDispatch.register(IPCTask.class);
	}
}
