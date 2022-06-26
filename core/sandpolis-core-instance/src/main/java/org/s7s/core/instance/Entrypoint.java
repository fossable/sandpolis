//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;
import org.s7s.core.foundation.S7SString;
import org.s7s.core.foundation.S7SString.AnsiColor;
import org.s7s.core.foundation.S7SSystem;
import org.s7s.core.instance.InitTask.TaskOutcome;
import org.s7s.core.foundation.Instance.InstanceFlavor;
import org.s7s.core.foundation.Instance.InstanceType;

/**
 * Main classes can inherit this class to have the instance setup automatically.
 */
public abstract class Entrypoint {

	public static record EntrypointInfo(

			/**
			 * The instance's main class.
			 */
			Class<?> main,

			/**
			 * The instance's type.
			 */
			InstanceType instance,

			/**
			 * The instance's subtype.
			 */
			InstanceFlavor flavor,

			/**
			 * The instance's UUID.
			 */
			String uuid,

			/**
			 * The instance jar path.
			 */
			Path jar) {
	}

	private final Logger log;

	private static EntrypointInfo metadata;

	public static EntrypointInfo data() {
		if (metadata == null) {
			throw new IllegalStateException();
		}
		return metadata;
	}

	/**
	 * A list of tasks that are executed periodically.
	 */
	private IdleLoop idle;

	/**
	 * A list of tasks that are executed on instance shutdown.
	 */
	private List<ShutdownTask> shutdown = new ArrayList<>();

	private boolean started = false;

	/**
	 * A list of tasks that initialize the instance.
	 */
	private List<InitTask> tasks = new ArrayList<>();

	protected Entrypoint(Class<?> main, InstanceType instance, InstanceFlavor flavor) {

		try {
			Entrypoint.metadata = new EntrypointInfo(main, instance, flavor, readUuid(instance, flavor).toString(),
					Paths.get(main.getProtectionDomain().getCodeSource().getLocation().toURI()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		log = LoggerFactory.getLogger(Entrypoint.class);
	}

	/**
	 * Build a summary of the task execution and write to log.
	 */
	private void logSummary(List<TaskOutcome> outcomes) {
		if (outcomes.isEmpty()) {
			return;
		}

		// Create a format string according to the width of the longest task description
		String descFormat = String.format("%%%ds:",
				Math.min(outcomes.stream().map(TaskOutcome::name).mapToInt(String::length).max().getAsInt(), 70));

		log.info("===== Initialization task summary =====");
		for (var outcome : outcomes) {

			// Format description and result
			String line = String.format(descFormat + " %4s", outcome.name(),
					outcome.skipped() ? S7SString.of("SKIP").colorize(AnsiColor.BLUE)
							: outcome.success() ? S7SString.of("OK").colorize(AnsiColor.GREEN)
									: S7SString.of("FAIL").colorize(AnsiColor.RED));

			// Format duration
			if (outcome.skipped() || !outcome.success())
				line += " ( ---- ms)";
			else if (outcome.duration() > 9999)
				line += String.format(" (%5.1f  s)", outcome.duration() / 1000.0);
			else
				line += String.format(" (%5d ms)", outcome.duration());

			// Write to log
			log.info(line);
		}

		// Log any failure messages/exceptions
		for (var outcome : outcomes) {
			if (!outcome.skipped()) {

				if (!outcome.success()) {
					if (outcome.exception() != null)
						log.error("An exception occurred in task \"{}\":\n{}", outcome.name(), outcome.exception());
					else if (outcome.reason() != null)
						log.error("An error occurred in task \"{}\": {}", outcome.name(), outcome.reason());
					else
						log.error("An unknown error occurred in task \"{}\"", outcome.name());
				}
			}
		}
	}

	private UUID readUuid(InstanceType instance, InstanceFlavor flavor) {
		int seed = instance.getNumber() << 24;
		seed |= flavor.getNumber() << 16;
		seed |= S7SSystem.OS_TYPE.getNumber();

		var uuid_hash = Hashing.murmur3_128(seed).newHasher();

		try {
			for (var i : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				if (!i.isLoopback() && !i.isVirtual()) {
					uuid_hash.putBytes(i.getHardwareAddress());
				}
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		var uuid_buffer = ByteBuffer.wrap(uuid_hash.hash().asBytes());

		return new UUID(uuid_buffer.getLong(), uuid_buffer.getLong());
	}

	public void register(InitTask task) {
		if (started)
			throw new IllegalStateException("Cannot register task");

		tasks.add(task);
	}

	public void start(String instanceName, String[] args) {
		if (started)
			throw new IllegalStateException("Start cannot be called more than once");

		started = true;

		final long timestamp = System.currentTimeMillis();

		if (BuildConfig.EMBEDDED != null) {
			log.info("Starting instance: {} ({})", S7SString.of(instanceName).rainbowize(),
					BuildConfig.EMBEDDED.versions().instance());
			log.debug("  Build Timestamp: {}", new Date(BuildConfig.EMBEDDED.timestamp()));
			log.debug("  Build Platform: {}", BuildConfig.EMBEDDED.platform());
			log.debug("  Build JVM: {}", BuildConfig.EMBEDDED.versions().java());
		}

		log.debug("  Runtime Platform: {} ({})", System.getProperty("os.name"), System.getProperty("os.arch"));
		log.debug("  Runtime JVM: {} ({})", System.getProperty("java.version"), System.getProperty("java.vendor"));
		log.debug("  Instance Type: {}", metadata.instance);
		log.debug("  Instance Type Flavor: {}", metadata.flavor);
		log.debug("  Instance UUID: {}", metadata.uuid);

		// Setup exception handler
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			log.error("An unexpected exception has occurred", throwable);
		});

		// Setup shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			shutdown.forEach(task -> {
				try {
					task.run();
				} catch (Exception e) {
					log.error("Failed to execute shutdown task", e);
				}
			});
		}));

		boolean initFailed = false;
		List<TaskOutcome> outcomes = new ArrayList<>();

		// Execute registered initialization tasks
		for (var task : tasks) {

			var outcome = TaskOutcome.Factory.of(task.description());

			// Skip if the entire run failed or the task is not enabled
			if (initFailed || !task.enabled()) {
				outcomes.add(outcome.skipped());
				continue;
			}

			// Execute the task
			try {
				outcomes.add(task.run(outcome));
			} catch (Exception e) {
				outcomes.add(outcome.failed(e));
			}

			// Exit if the task failed and fatal is enabled
			if (!outcomes.get(outcomes.size() - 1).skipped() && !outcomes.get(outcomes.size() - 1).success()
					&& task.fatal()) {
				initFailed = true;
			}
		}

		if (initFailed) {
			logSummary(outcomes);
			System.exit(1);
		}

		// Print task summary if required
		logSummary(outcomes);

		// Launch idle loop
		if (idle != null)
			idle.start();

		log.info("Initialization completed in {} ms", System.currentTimeMillis() - timestamp);
	}
}
