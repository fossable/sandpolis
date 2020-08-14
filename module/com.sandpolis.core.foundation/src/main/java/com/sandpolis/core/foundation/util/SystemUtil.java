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
package com.sandpolis.core.foundation.util;

import static com.sandpolis.core.foundation.Platform.OsType.AIX;
import static com.sandpolis.core.foundation.Platform.OsType.LINUX;
import static com.sandpolis.core.foundation.Platform.OsType.DARWIN;
import static com.sandpolis.core.foundation.Platform.OsType.BSD;
import static com.sandpolis.core.foundation.Platform.OsType.SOLARIS;
import static com.sandpolis.core.foundation.Platform.OsType.UNRECOGNIZED;
import static com.sandpolis.core.foundation.Platform.OsType.WINDOWS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;
import com.sandpolis.core.foundation.Platform.OsType;

public final class SystemUtil {

	private static final Logger log = LoggerFactory.getLogger(SystemUtil.class);

	public static final OsType OS_TYPE = queryOsType();

	/**
	 * Detect the OS type of the current system.
	 *
	 * @return The system's {@link OsType} or {@code UNRECOGNIZED}
	 */
	private static OsType queryOsType() {
		String name = System.getProperty("os.name").toLowerCase();

		if (name.startsWith("windows"))
			return WINDOWS;

		if (name.startsWith("linux"))
			return LINUX;

		if (name.startsWith("mac") || name.startsWith("darwin"))
			return DARWIN;

		if (name.startsWith("solaris") || name.startsWith("sunos"))
			return SOLARIS;

		if (name.startsWith("freebsd") || name.startsWith("openbsd"))
			return BSD;

		if (name.startsWith("aix"))
			return AIX;

		return UNRECOGNIZED;
	}

	public static final class ProcessWrapper {

		private Process process;
		private Exception error;

		private ProcessWrapper(Process process) {
			this.process = Objects.requireNonNull(process);
		}

		private ProcessWrapper(Exception e) {
			this.error = Objects.requireNonNull(e);
		}

		public ProcessWrapper complete(long timeout) throws Exception {
			if (error != null)
				throw error;

			process.waitFor(timeout, TimeUnit.SECONDS);
			return this;
		}

		public int exitValue() throws Exception {
			return process.waitFor();
		}

		public Stream<String> lines() {
			return new BufferedReader(new InputStreamReader(process.getInputStream())).lines();
		}

		public String string() {
			try {
				return CharStreams.toString(new InputStreamReader(process.getInputStream()));
			} catch (IOException e) {
				error = e;
				return "";
			}
		}

	}

	public static ProcessWrapper exec(String... cmd) {
		log.trace("Executing system command: \"{}\"", String.join(" ", cmd));

		try {
			return new ProcessWrapper(Runtime.getRuntime().exec(cmd));
		} catch (IOException e) {
			// A failed ProcessWrapper
			return new ProcessWrapper(e);
		}
	}

	public static ProcessWrapper execp(String... cmd) {
		// TODO use OS type
		return exec(cmd);
	}

	private SystemUtil() {
	}
}
