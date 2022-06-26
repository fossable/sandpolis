//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ObjectArrays;
import com.google.common.io.CharStreams;

/**
 * Wrapper for {@link Process} that provides a simple interface without checked
 * exceptions.
 */
public record S7SProcess(Process process) {

	private static final Logger log = LoggerFactory.getLogger(S7SProcess.class);

	public static interface CompletionHandler {
		public void complete(int exit, String stdout, String stderr);
	}

	public static class ProcessException extends RuntimeException {

		private ProcessException(InterruptedException e) {
			super(e);
		}

		private ProcessException(IOException e) {
			super(e);
		}

		private ProcessException(String message) {
			super(message);
		}
	}

	/**
	 * Start a new process.
	 *
	 * @param executable The process executable
	 * @param cmdLine    Arguments
	 * @return A new {@link S7SProcess}
	 */
	public static S7SProcess exec(Path executable, String... cmdLine) {
		return exec(ObjectArrays.concat(executable.toString(), cmdLine));
	}

	/**
	 * Start a new process.
	 *
	 * @param cmdLine The process executable and arguments
	 * @return A new {@link S7SProcess}
	 */
	public static S7SProcess exec(String... cmdLine) {

		if (log.isTraceEnabled())
			log.trace("Starting new process: \"{}\"", String.join(" ", cmdLine));

		try {
			return new S7SProcess(Runtime.getRuntime().exec(cmdLine));
		} catch (IOException e) {
			throw new ProcessException(e);
		}
	}

	/**
	 * Start a new process as a superuser. If the current user is not the superuser,
	 * then the platform specific "run as" mechanism will be invoked (not
	 * necessarily "sudo").
	 *
	 * @param cmdLine The process executable and arguments
	 * @return A new {@link S7SProcess}
	 */
	public static S7SProcess sudo(String... cmdLine) {

		switch (S7SSystem.OS_TYPE) {
		case FREEBSD:
		case MACOS:
		case LINUX:

			if (System.getProperty("user.name").equals("root")) {
				return exec(cmdLine);
			}

			var sudo = S7SFile.which("sudo");
			if (sudo.isPresent()) {
				log.trace("Using 'sudo' mechanism");
				return exec(sudo.get().path(), ObjectArrays.concat("-n", cmdLine));
			}

			break;
		case WINDOWS:

			// Check if we're admin
			if (exec("net", "user", System.getProperty("user.name")).stdout().contains("*Administrators")) {
				return exec(cmdLine);
			}

			var runas = S7SFile.which("runas.exe");
			if (runas.isPresent()) {
				log.trace("Using 'runas' mechanism");
				return exec(runas.get().path(), ObjectArrays.concat("/user:administrator", cmdLine));
			}
			break;
		default:
			break;
		}

		throw new ProcessException("Failed to find 'run as' mechanism");
	}

	public Stream<String> stdoutLines() {
		return new BufferedReader(new InputStreamReader(process.getInputStream())).lines();
	}

	public Stream<String> stderrLines() {
		return new BufferedReader(new InputStreamReader(process.getErrorStream())).lines();
	}

	/**
	 * Write to the process's stdin.
	 *
	 * @param input The desired input
	 * @return {@code this}
	 */
	public S7SProcess stdin(String input) {
		try {
			process.getOutputStream().write(input.getBytes());
			process.getOutputStream().flush();
		} catch (IOException e) {
			throw new ProcessException(e);
		}
		return this;
	}

	/**
	 * @return The process's full stdout
	 */
	public String stdout() {
		try {
			return CharStreams.toString(new InputStreamReader(process.getInputStream()));
		} catch (IOException e) {
			throw new ProcessException(e);
		}
	}

	/**
	 * @return The process's full stderr
	 */
	public String stderr() {
		try {
			return CharStreams.toString(new InputStreamReader(process.getErrorStream()));
		} catch (IOException e) {
			throw new ProcessException(e);
		}
	}

	/**
	 * Wait for the process to complete.
	 *
	 * @return The process exit code
	 */
	public int complete() {
		try {
			return process.waitFor();
		} catch (InterruptedException e) {
			throw new ProcessException(e);
		}
	}

	/**
	 * Wait for the process to complete.
	 *
	 * @param handler Handler for the stdout, stderr, and exit code
	 */
	public void complete(CompletionHandler handler) {
		int exit = complete();
		handler.complete(exit, stdout(), stderr());
	}

	/**
	 * Run the given handler when the process completes.
	 *
	 * @param handler Handler for the stdout, stderr, and exit code
	 */
	public void onComplete(CompletionHandler handler) {
		// Simple, but not scalable
		new Thread(() -> {
			complete(handler);
		}).start();
	}

}
