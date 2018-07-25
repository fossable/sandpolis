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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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

	private static List<Runnable> shutdown = new ArrayList<>();

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

		// Setup exception handler
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			log.error("An unexpected exception has occurred", throwable);
		});

		try {
			main.getDeclaredMethod("main", String[].class).invoke(null, (Object) args);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException("Failed to invoke main() in class: " + main.getName(), e);
		}

		// Setup shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			shutdown.forEach(task -> task.run());
			shutdown = null;
		}));

		OutcomeSet outcomes = new OutcomeSet();

		// Run configuration
		for (Supplier<Outcome> task : tasks)
			outcomes.add(task.get());

		// Print nonfatal errors
		if (!outcomes.getResult())
			for (Outcome failed : outcomes.getFailed()) {
				if (failed.getException() != null)
					log.error("({}) {}", failed.getAction(), failed.getException());
				else
					log.error("({}) {}", failed.getAction(), failed.getComment());
			}

		// Launch idle loop
		if (idle != null)
			idle.start();

		log.info("Initialization completed in {} ms", outcomes.getTime());

		// Cleanup
		tasks = null;
	}

	/**
	 * Register a new initialization task which will be executed during the
	 * dispatch. Tasks registered with this method are executed sequentially in the
	 * same order as the method calls.
	 * 
	 * @param task   The task reference
	 * @param config The configuration annotation
	 */
	public static void register(Supplier<Outcome> task, InitializationTask config) {
		if (task == null)
			throw new IllegalArgumentException();
		if (config == null)
			throw new IllegalArgumentException();
		if (tasks == null)
			throw new IllegalStateException("Tasks cannot be registered after dispatch is complete");
		if (tasks.contains(task))
			throw new IllegalArgumentException("Tasks cannot be registered more than once");

		tasks.add(() -> {
			Outcome outcome = task.get();
			if (outcome == null)
				throw new RuntimeException("Invalid task detected");

			if (!outcome.getResult() && config.fatal()) {
				log.error("A fatal error has occurred in task \"{}\": {}", outcome.getAction(), outcome.getComment());
				throw new RuntimeException();
			}
			return outcome;
		});
	}

	/**
	 * Register a new initialization task which will be executed during the
	 * dispatch. Tasks registered with this method are executed sequentially in the
	 * same order as the method calls.
	 * 
	 * @param task     The task reference
	 * @param source   The source class
	 * @param taskName The name of the
	 */
	public static void register(Supplier<Outcome> task, Class<?> source, String taskName) {
		if (source == null)
			throw new IllegalArgumentException();

		try {
			register(task, source.getDeclaredMethod(taskName).getAnnotation(InitializationTask.class));
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Missing initialization annotation");
		}
	}

	/**
	 * Register an {@link IdleLoop} with {@link MainDispatch}. The loop will be
	 * started during dispatch.
	 * 
	 * @param idle The new {@link IdleLoop}
	 */
	public static void register(IdleLoop idle) {
		if (idle == null)
			throw new IllegalArgumentException();
		if (MainDispatch.idle != null)
			throw new IllegalStateException("Only one idle loop can be registered");

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

	/**
	 * Register a task to run during shutdown. Tasks will be executed in the same
	 * order as registration.
	 * 
	 * @param task The shutdown task
	 */
	public static void registerShutdown(Runnable task) {
		if (shutdown.contains(task))
			throw new IllegalArgumentException("Shutdown tasks cannot be registered more than once");

		shutdown.add(task);
	}

	/**
	 * A task that is executed during instance shutdown.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface ShutdownTask {
	}

	/**
	 * A task that is executed at an indeterminate time in the future when the
	 * instance has relatively little work to do.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface IdleTask {
	}

	/**
	 * A task that is executed during instance initialization.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface InitializationTask {

		/**
		 * Indicates that the application should exit if the task fails.
		 */
		public boolean fatal() default false;
	}

}
