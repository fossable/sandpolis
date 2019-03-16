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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.TaskOutcome;

/**
 * Contains general tasks useful to multiple instances. This class allows common
 * tasks to be specified only once.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class BasicTasks {

	private static final Logger log = LoggerFactory.getLogger(BasicTasks.class);

	/**
	 * Load the configuration from the runtime environment.
	 *
	 * @return The task's outcome
	 */
	@InitializationTask(name = "Load instance configuration", fatal = true)
	public static TaskOutcome loadConfiguration() {
		TaskOutcome task = TaskOutcome.begin(new Object() {
		}.getClass().getEnclosingMethod());

		Config.register("post", true);

		Config.register("db.provider", "hibernate");
		Config.require("db.url", String.class);
		Config.require("db.username", String.class);
		Config.require("db.password", String.class);

		Config.register("no_mutex", false);
		Config.register("log.traffic", false);
		Config.register("log.traffic_raw", false);
		Config.register("log.startup_summary", false);

		Config.register("net.tls", false);
		Config.register("net.timeout.response.default", 2000);

		Config.register("no_plugins", false);
		Config.register("no_summary", false);

		Config.register("path.db", null);
		Config.register("path.gen", null);
		Config.register("path.log", null);

		return task.success();
	}

	private BasicTasks() {
	}
}
