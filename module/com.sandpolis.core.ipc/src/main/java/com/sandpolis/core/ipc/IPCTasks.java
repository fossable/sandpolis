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
package com.sandpolis.core.ipc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.ConfigConstant.net;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.TaskOutcome;
import com.sandpolis.core.ipc.store.IPCStore;
import com.sandpolis.core.ipc.MCMetadata.RS_Metadata;

/**
 * Contains IPC tasks useful to multiple instances.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class IPCTasks {

	private static final Logger log = LoggerFactory.getLogger(IPCTasks.class);

	/**
	 * Check for existing instance locks. If found, this instance will exit.
	 * Otherwise a new lock is established.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Check instance locks", fatal = true)
	public static TaskOutcome checkLocks() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		if (!Config.getBoolean(net.ipc.mutex))
			return task.skipped();

		RS_Metadata metadata = IPCStore.queryInstance(Core.INSTANCE);
		if (metadata != null) {
			return task.failure("Another instance has been detected (process " + metadata.getPid() + ")");
		}

		try {
			IPCStore.listen(Core.INSTANCE);
		} catch (IOException e) {
			log.warn("Failed to initialize an IPC listener", e);
		}

		return task.success();
	}

	private IPCTasks() {
	}
}
