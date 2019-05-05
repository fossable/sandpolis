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
package com.sandpolis.core.instance;

import java.util.concurrent.Executors;

import com.sandpolis.core.instance.ConfigConstant.logging;
import com.sandpolis.core.instance.ConfigConstant.net;
import com.sandpolis.core.instance.ConfigConstant.path;
import com.sandpolis.core.instance.ConfigConstant.plugin;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.TaskOutcome;
import com.sandpolis.core.instance.store.thread.ThreadStore;

/**
 * Contains general tasks useful to multiple instances. This class allows common
 * tasks to be specified only once.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class BasicTasks {

	/**
	 * Load the configuration from the runtime environment.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load instance configuration", fatal = true)
	public static TaskOutcome loadConfiguration() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		Config.register(ConfigConstant.post, true);

		Config.register(net.ipc.mutex, true);
		Config.register(net.ipc.timeout, 400);
		Config.register(logging.net.traffic.decoded, false);
		Config.register(logging.net.traffic.raw, false);
		Config.register(logging.startup.summary, false);

		Config.register(net.connection.outgoing.pool_size, 2);
		Config.register(net.connection.tls, false);
		Config.register(net.message.default_timeout, 2000);

		Config.register(plugin.enabled, true);

		Config.register(path.log);

		return task.success();
	}

	/**
	 * Load static stores.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load instance stores", fatal = true)
	public static TaskOutcome loadStores() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		ThreadStore.register(Executors.newSingleThreadExecutor(), PoolConstant.signaler);
		Signaler.init(ThreadStore.get(PoolConstant.signaler));

		return task.success();
	}

	private BasicTasks() {
	}
}
