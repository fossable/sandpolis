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
package com.sandpolis.core.ipc.task;

import static com.sandpolis.core.ipc.IPCStore.IPCStore;

import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.Task;
import com.sandpolis.core.ipc.Metadata.RS_Metadata;

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
	@InitializationTask(name = "Load IPC module", condition = "net.ipc.mutex", fatal = true)
	public static final Task load = new Task((task) -> {
		IPCStore.init(config -> {
			config.ephemeral();
		});

		return task.success();
	});

	/**
	 * Check for an existing instance lock. If found, this instance will exit.
	 */
	@InitializationTask(name = "Check instance lock", condition = "net.ipc.mutex", fatal = true)
	public static final Task checkLock = new Task((task) -> {

		RS_Metadata metadata = IPCStore.queryInstance(Core.INSTANCE, Core.FLAVOR).orElse(null);
		if (metadata != null)
			return task.failure("Another instance has been detected (process " + metadata.getPid() + ")");

		return task.success();
	});

	/**
	 * Set a new instance lock.
	 */
	@InitializationTask(name = "Set instance lock", condition = "net.ipc.mutex", fatal = false)
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
