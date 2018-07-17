/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.idle.IdleLoop;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Result.Outcome;

/**
 * This class invokes an instance's real {@code main()} via reflection and
 * assists with initialization of the new instance.<br>
 * <br>
 * The {@link #dispatch} method should always be invoked first
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class MainDispatch {
	private MainDispatch() {
	}

	public static final Logger log = LoggerFactory.getLogger(MainDispatch.class);

	/**
	 * A configurable list of tasks that are executed when the instance's
	 * {@code main()} returns.
	 */
	private static List<Supplier<Outcome>> tasks = new ArrayList<>();

	/**
	 * The {@link Thread} that runs
	 */
	private static IdleLoop idle;

	/**
	 * The instance's main {@code Class}.
	 */
	private static Class<?> main = MainDispatch.class;

	/**
	 * The instance's {@code Instance} type.
	 */
	private static Instance instance;

	/**
	 * Get the main {@link Class} that was dispatched or {@code null} if
	 * {@link #dispatch} has not been called.
	 * 
	 * @return The dispatched {@link Class} or {@code null}
	 */
	public static Class<?> getMain() {
		return main;
	}

	/**
	 * Get the {@link Instance} that was dispatched or {@code null} if
	 * {@link #dispatch} has not been called.
	 * 
	 * @return The dispatched {@link Instance} or {@code null}
	 */
	public static Instance getInstance() {
		return instance;
	}

	/**
	 * Get the {@link IdleLoop} or {@code null} if one has not been registered.
	 * 
	 * @return The registered {@link IdleLoop} or {@code null}
	 */
	public static IdleLoop getIdleLoop() {
		return idle;
	}

	/**
	 * Invokes the instance's {@code main()} method (which should register
	 * initialization tasks with {@link MainDispatch}) and then initializes the
	 * instance.
	 * 
	 * 
	 * @param main     The {@link Class} which contains the {@code main()} to invoke
	 * @param args     The arguments to be passed to the {@code main()}
	 * @param instance The instance's {@link Instance}
	 */
	public static void dispatch(Class<?> main, String[] args, Instance instance) {
		if (main == null)
			throw new IllegalArgumentException();
		if (args == null)
			throw new IllegalArgumentException();
		if (instance == null)
			throw new IllegalArgumentException();

		if (MainDispatch.instance != null)
			throw new IllegalStateException("Dispatch cannot be called more than once");

		MainDispatch.main = main;
		MainDispatch.instance = instance;

		try {
			main.getDeclaredMethod("main", String[].class).invoke(null, (Object) args);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException("Failed to invoke main() in class: " + main.getName(), e);
		}

		OutcomeSet outcomes = new OutcomeSet();

		// Run configuration
		for (Supplier<Outcome> task : tasks)
			outcomes.add(task.get());

		// Print nonfatal errors
		if (!outcomes.getResult())
			for (Outcome failed : outcomes.getFailed())
				log.error("({}) {}", failed.getAction(), failed.getComment());

		// Launch idle loop
		if (idle != null)
			idle.start();

		log.info("Initialization completed in {} ms", outcomes.getTime());

		// Cleanup
		tasks = null;
	}

	/**
	 * Register an initialization task with the given attributes.
	 * 
	 * @param task      The initialization task
	 * @param condition A condition that determines whether the task should run
	 * @param fatal     Indicates whether {@code System.exit} should be called if
	 *                  the task fails
	 */
	public static void register(Supplier<Outcome> task, Supplier<Boolean> condition, boolean fatal) {
		if (tasks == null)
			throw new IllegalStateException("Tasks cannot be registered after dispatch is complete");

		tasks.add(() -> {
			if (condition.get()) {
				Outcome outcome = task.get();
				if (outcome == null)
					throw new RuntimeException("Invalid task detected");

				if (!outcome.getResult() && fatal) {
					log.error("A fatal error has occurred in task \"{}\": {}", outcome.getAction(),
							outcome.getComment());
					throw new RuntimeException();
				}
				return outcome;
			} else
				return Outcome.newBuilder().setResult(true).setComment("Skipped").build();
		});
	}

	/**
	 * Register an initialization task with default attributes.
	 * 
	 * @param task The initialization task
	 */
	public static void register(Supplier<Outcome> task) {
		register(task, () -> true, false);
	}

	/**
	 * Register an {@link IdleLoop} with {@link MainDispatch}.
	 * 
	 * @param idle The new {@link IdleLoop}
	 */
	public static void register(IdleLoop idle) {
		if (idle != null)
			throw new IllegalStateException();

		MainDispatch.idle = idle;
	}

	/**
	 * Register an idle task with the {@link IdleLoop}.
	 * 
	 * @param task The new idle task which returns true if the task should be
	 *             rescheduled after completion or false if the task should be
	 *             dropped
	 */
	public static void registerIdle(Supplier<Boolean> task) {
		if (idle == null)
			throw new IllegalStateException();

		idle.register(task);
	}

}
