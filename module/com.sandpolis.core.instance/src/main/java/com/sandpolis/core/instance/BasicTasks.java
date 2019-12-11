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
package com.sandpolis.core.instance;

import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.Task;

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
	 */
	@InitializationTask(name = "Load instance configuration", fatal = true)
	public static final Task loadConfiguration = new Task((task) -> {
		Config.register("post", true);

		Config.register("traffic.interval", 4000);
		Config.register("net.ipc.mutex", true);
		Config.register("net.ipc.timeout", 400);
		Config.register("logging.net.traffic.decoded", false);
		Config.register("logging.net.traffic.raw", false);
		Config.register("logging.startup.summary", false);

		Config.register("net.connection.outgoing.pool_size", 2);
		Config.register("net.connection.tls", true);
		Config.register("net.message.default_timeout", 2000);

		Config.register("plugin.enabled", true);

		Config.register("path.lib");
		Config.register("path.log");
		Config.register("path.plugin");
		Config.register("path.tmp");

		return task.success();
	});

	private BasicTasks() {
	}

	static {
		MainDispatch.register(BasicTasks.class);
	}
}
