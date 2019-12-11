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
package com.sandpolis.core.instance.idle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link IdleLoop} runs periodic tasks in the background when the instance
 * is idle.<br>
 * <br>
 * Like CPU scheduling, no assumptions can be made about the order or timing of
 * idle task execution.
 *
 * @author cilki
 * @since 5.0.0
 */
public class IdleLoop {

	private static final Logger log = LoggerFactory.getLogger(IdleLoop.class);

	/**
	 *
	 */
	private static final int REFRESH = 1000 * 60 * 15;

	/**
	 * Tasks that should be performed on the idle loop periodically.
	 */
	private List<Supplier<Boolean>> tasks = Collections.synchronizedList(new ArrayList<>());

	/**
	 * The control thread.
	 */
	private Thread thread;

	public IdleLoop() {

		// Setup control thread
		thread = new Thread(() -> {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					Thread.sleep(REFRESH);
					tasks.removeIf(task -> !task.get());
				}
			} catch (InterruptedException ignore) {
			}
			log.debug("Terminating idle loop");
		});

		thread.setPriority(Thread.MIN_PRIORITY);
		thread.setDaemon(true);
	}

	/**
	 * Add a new task to the idle loop.
	 *
	 * @param task The new idle task which returns true if the task should be
	 *             rescheduled after completion or false if the task should be
	 *             dropped
	 */
	public void register(Supplier<Boolean> task) {
		if (task == null)
			throw new IllegalArgumentException();
		if (tasks.contains(task))
			throw new IllegalArgumentException();

		tasks.add(task);
	}

	/**
	 * Begin the idle thread.
	 */
	public void start() {
		log.debug("Starting idle loop");
		thread.start();
	}

}
