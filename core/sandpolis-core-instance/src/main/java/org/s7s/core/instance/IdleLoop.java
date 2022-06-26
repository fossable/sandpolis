//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link IdleLoop} runs periodic tasks in the background when the instance
 * is "idle".
 *
 * <p>
 * No assumptions can be made about the order or timing of task execution on an
 * {@link IdleLoop}. Tasks should not block the {@link IdleLoop} for long
 * periods of time.
 *
 * <p>
 * Idle tasks return {@code true} if they should be rescheduled after completion
 * or {@code false} if the task should be removed from the loop.
 *
 * @author cilki
 * @since 5.0.0
 */
public class IdleLoop {

	private static final Logger log = LoggerFactory.getLogger(IdleLoop.class);

	private static final int REFRESH = 1000 * 60 * 15;

	/**
	 * Tasks that should be performed on the idle loop periodically.
	 */
	private List<IdleTask> tasks = Collections.synchronizedList(new ArrayList<>());

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
					tasks.removeIf(task -> {
						try {
							return !task.run();
						} catch (Exception e) {
							log.error("Exception occured in idle task", e);
							return false;
						}
					});
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
	 * @param task The new idle task
	 */
	public void register(IdleTask task) {
		if (task == null)
			throw new IllegalArgumentException();
		if (tasks.contains(task))
			throw new IllegalArgumentException();

		tasks.add(task);
	}

	/**
	 * Start the idle loop.
	 */
	public void start() {
		checkState(!thread.isAlive());

		log.debug("Starting idle loop: {}", toString());
		thread.start();
	}

	@Override
	public String toString() {
		return thread.getName() + "@" + tasks.size();
	}
}
