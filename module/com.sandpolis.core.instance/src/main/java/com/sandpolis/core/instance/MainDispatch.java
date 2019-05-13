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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.ConfigConstant.logging;
import com.sandpolis.core.instance.idle.IdleLoop;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.util.ProtoUtil;

/**
 * {@link MainDispatch} allows the instance's main method to configure
 * initialization tasks before they are sequentially executed by this class.
 * Idle tasks and shutdown tasks are also managed by this class.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class MainDispatch {

	public static final Logger log = LoggerFactory.getLogger(MainDispatch.class);

	/**
	 * A configurable list of tasks that initialize the instance.
	 */
	private static List<Task> tasks = new ArrayList<>();

	/**
	 * A configurable list of tasks that are executed on instance shutdown.
	 */
	private static List<Runnable> shutdown = new LinkedList<>();

	/**
	 * A {@link Thread} that runs idle tasks in the background.
	 */
	private static IdleLoop idle;

	/**
	 * The instance's main {@link Class}.
	 */
	private static Class<?> main = MainDispatch.class;

	/**
	 * The instance's {@link Instance} type.
	 */
	private static Instance instance;

	/**
	 * The instance's {@link InstanceFlavor} type.
	 */
	private static InstanceFlavor flavor;

	/**
	 * Get the {@link Class} that was dispatched.
	 * 
	 * @return The dispatched {@link Class} or {@code MainDispatch.class} if
	 *         {@link #dispatch} has not been called
	 */
	public static Class<?> getMain() {
		return main;
	}

	/**
	 * Get the {@link Instance} type.
	 * 
	 * @return The dispatched {@link Instance} or {@code null} if {@link #dispatch}
	 *         has not been called
	 */
	public static Instance getInstance() {
		return instance;
	}

	/**
	 * Get the {@link InstanceFlavor} type.
	 * 
	 * @return The dispatched {@link InstanceFlavor} or {@code null} if
	 *         {@link #dispatch} has not been called
	 */
	public static InstanceFlavor getInstanceFlavor() {
		return flavor;
	}

	/**
	 * Get the {@link IdleLoop}.
	 * 
	 * @return The registered {@link IdleLoop} or {@code null} if one has not been
	 *         registered
	 */
	public static IdleLoop getIdleLoop() {
		return idle;
	}

	/**
	 * Invokes the instance's main method (which should register initialization
	 * tasks with {@link MainDispatch}) and then initializes the instance.
	 * 
	 * @param main     The {@link Class} which contains the {@code main} to invoke
	 * @param args     The arguments to be passed to the {@code main}
	 * @param instance The instance's {@link Instance}
	 * @param flavor   The instance's {@link InstanceFlavor}
	 */
	public static void dispatch(Class<?> main, String[] args, Instance instance, InstanceFlavor flavor) {
		if (MainDispatch.main != MainDispatch.class)
			throw new IllegalStateException("Dispatch cannot be called more than once");

		MainDispatch.main = Objects.requireNonNull(main);
		MainDispatch.instance = Objects.requireNonNull(instance);
		MainDispatch.flavor = Objects.requireNonNull(flavor);

		long timestamp = System.currentTimeMillis();

		// Pass main arguments to the Config class
		Config.setArguments(args);

		// Setup exception handler
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			log.error("An unexpected exception has occurred", throwable);
		});

		// Setup shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			shutdown.forEach(task -> task.run());
		}));

		// Invoke the main method
		try {
			main.getDeclaredMethod("main", String[].class).invoke(null, (Object) args);
		} catch (Exception e) {
			throw new RuntimeException("Failed to invoke main method in class: " + main.getName(), e);
		}

		// Execute tasks
		for (Task task : tasks) {
			if (task.initMetadata == null)
				throw new RuntimeException("Unregistered initialization task class");

			TaskOutcome outcome = new TaskOutcome(task.initMetadata.name());
			task.outcome = outcome;

			if (!task.initMetadata.condition().isEmpty() && !Config.getBoolean(task.initMetadata.condition())) {
				outcome.skipped = true;
			} else {
				try {
					task.execute(outcome);
				} catch (Exception e) {
					outcome.failure(e);
				}
			}

			if (!outcome.getOutcome().getResult() && task.initMetadata.fatal() && !outcome.isSkipped()) {
				log.error("A fatal error has occurred in task: {}", task.initMetadata.name());
				logTaskSummary();
				System.exit(0);
			}
		}

		// Print task summary if any task failed
		if (tasks.stream().filter(t -> !t.outcome.getOutcome().getResult()).count() != 0)
			logTaskSummary();

		// Print task summary if required
		else if (!Config.has(logging.startup.summary) || !Config.getBoolean(logging.startup.summary))
			logTaskSummary();

		// Launch idle loop
		if (idle != null)
			idle.start();

		// Cleanup
		tasks = null;

		log.info("Initialization completed in {} ms", System.currentTimeMillis() - timestamp);
	}

	/**
	 * Build a summary for {@link #tasks} and write to log.
	 */
	private static void logTaskSummary() {
		if (tasks.isEmpty())
			return;

		// Create a format string according to the width of the longest description
		String format = String.format("%%%ds: %%4s (%%5d ms)",
				tasks.stream().mapToInt(task -> task.initMetadata.name().length()).max().getAsInt());

		for (Task task : tasks) {
			TaskOutcome taskOutcome = task.getOutcome();
			Outcome outcome = taskOutcome.getOutcome();

			String line = String.format(format, outcome.getAction(),
					taskOutcome.isSkipped() ? "SKIP" : outcome.getResult() ? "OK" : "FAIL", outcome.getTime());
			if (taskOutcome.isSkipped() || outcome.getResult()) {
				log.info(line);
			} else {
				log.error(line);
				if (!outcome.getException().isEmpty())
					log.error(outcome.getException());
			}
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
			// Register a default loop
			idle = new IdleLoop();

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
	 * Register a new initialization task which will be executed during the
	 * dispatch. Tasks registered with this method are executed sequentially in the
	 * same order as the method calls.
	 * 
	 * @param task The task reference
	 */
	public static void register(Task task) {
		Objects.requireNonNull(task);
		if (tasks == null)
			throw new IllegalStateException("Tasks cannot be registered after dispatch is complete");
		if (tasks.contains(task))
			throw new IllegalArgumentException("Tasks cannot be registered more than once");

		tasks.add(task);
	}

	/**
	 * A task that is executed during instance shutdown.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface ShutdownTask {
	}

	/**
	 * A task that is executed at an indeterminate time in the future when the
	 * instance has relatively little work to do.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface IdleTask {
	}

	/**
	 * A task that is executed during instance initialization.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface InitializationTask {

		/**
		 * The name of the task.
		 */
		public String name();

		/**
		 * The condition under which the task will execute.
		 */
		public String condition() default "";

		/**
		 * Indicates that the application should exit if the task fails.
		 */
		public boolean fatal() default false;

		/**
		 * Indicates that the task will run if and only if the instance is in debug
		 * mode.
		 */
		public boolean debug() default false;

	}

	/**
	 * Represents the outcome of an {@link InitializationTask}.
	 */
	public static class TaskOutcome {

		private Outcome outcome;
		private Outcome.Builder temporary;

		private boolean skipped;

		/**
		 * Get the task's skipped flag.
		 * 
		 * @return Whether the task was skipped
		 */
		public boolean isSkipped() {
			return skipped;
		}

		/**
		 * Get the task's outcome.
		 * 
		 * @return The task's outcome
		 */
		public Outcome getOutcome() {
			return outcome;
		}

		/**
		 * Mark the task as skipped.
		 * 
		 * @return A completed {@link TaskOutcome}
		 */
		public TaskOutcome skipped() {
			if (outcome != null)
				throw new IllegalStateException();

			skipped = true;
			outcome = ProtoUtil.complete(temporary);
			return this;
		}

		/**
		 * Mark the task as succeeded.
		 * 
		 * @return A completed {@link TaskOutcome}
		 */
		public TaskOutcome success() {
			if (outcome != null)
				throw new IllegalStateException();

			outcome = ProtoUtil.success(temporary);
			return this;
		}

		/**
		 * Mark the task as failed.
		 * 
		 * @return A completed {@link TaskOutcome}
		 */
		public TaskOutcome failure() {
			if (outcome != null)
				throw new IllegalStateException();

			outcome = ProtoUtil.failure(temporary);
			return this;
		}

		/**
		 * Mark the task as failed with an exception.
		 * 
		 * @param t The relevant exception
		 * @return A completed {@link TaskOutcome}
		 */
		public TaskOutcome failure(Exception t) {
			if (outcome != null)
				throw new IllegalStateException();

			outcome = ProtoUtil.failure(temporary, t);
			return this;
		}

		/**
		 * Mark the task as failed with a comment.
		 * 
		 * @param comment The relevant comment
		 * @return A completed {@link TaskOutcome}
		 */
		public TaskOutcome failure(String comment) {
			if (outcome != null)
				throw new IllegalStateException();

			outcome = ProtoUtil.failure(temporary, comment);
			return this;
		}

		/**
		 * Mark the task as complete.
		 * 
		 * @param result The task result
		 * @return A completed {@link TaskOutcome}
		 */
		public TaskOutcome complete(boolean result) {
			if (outcome != null)
				throw new IllegalStateException();

			outcome = ProtoUtil.complete(temporary.setResult(result));
			return this;
		}

		/**
		 * Mark the task as complete and merge the given outcome.
		 * 
		 * @param _outcome The outcome to merge
		 * @return A completed {@link TaskOutcome}
		 */
		public TaskOutcome complete(Outcome _outcome) {
			if (outcome != null)
				throw new IllegalStateException();

			outcome = temporary.clearTime().mergeFrom(_outcome).build();
			return this;
		}

		public TaskOutcome(String name) {
			temporary = ProtoUtil.begin(Objects.requireNonNull(name));
		}
	}

	/**
	 * The task's operation.
	 */
	public interface TaskAction {
		public TaskOutcome execute(TaskOutcome task) throws Exception;
	}

	public static class Task implements TaskAction {

		private InitializationTask initMetadata;
		private IdleTask idleMetadata;
		private ShutdownTask shutdownMetadata;

		private TaskAction action;
		private TaskOutcome outcome;

		public Task(TaskAction action) {
			this.action = Objects.requireNonNull(action);
		}

		public TaskOutcome getOutcome() {
			return outcome;
		}

		@Override
		public TaskOutcome execute(TaskOutcome task) throws Exception {
			return action.execute(task);
		}
	}

	/**
	 * Serach for task fields in the given class and inject annotations into them.
	 * 
	 * @param c The class to search
	 */
	public static void register(Class<?> c) {
		try {
			for (Field field : c.getDeclaredFields()) {
				for (Annotation annotation : field.getAnnotations()) {
					Field inject = null;
					if (annotation instanceof InitializationTask) {
						inject = Task.class.getDeclaredField("initMetadata");
					} else if (annotation instanceof IdleTask) {
						inject = Task.class.getDeclaredField("idleMetadata");
					} else if (annotation instanceof ShutdownTask) {
						inject = Task.class.getDeclaredField("shutdownMetadata");
					} else {
						continue;
					}

					field.setAccessible(true);
					inject.setAccessible(true);
					inject.set(field.get(null), annotation);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private MainDispatch() {
	}
}
