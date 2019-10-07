/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.core.instance.util;

import static com.sandpolis.core.proto.util.Platform.OsType.AIX;
import static com.sandpolis.core.proto.util.Platform.OsType.ANDROID;
import static com.sandpolis.core.proto.util.Platform.OsType.FREEBSD;
import static com.sandpolis.core.proto.util.Platform.OsType.LINUX;
import static com.sandpolis.core.proto.util.Platform.OsType.MACOS;
import static com.sandpolis.core.proto.util.Platform.OsType.OPENBSD;
import static com.sandpolis.core.proto.util.Platform.OsType.SOLARIS;
import static com.sandpolis.core.proto.util.Platform.OsType.UNRECOGNIZED;
import static com.sandpolis.core.proto.util.Platform.OsType.WINDOWS;

import com.sandpolis.core.proto.util.Platform.OsType;

public final class PlatformUtil {

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
			if ("dalvik".equalsIgnoreCase(System.getProperty("java.vm.name")))
				return ANDROID;
			else
				return LINUX;

		if (name.startsWith("mac") || name.startsWith("darwin"))
			return MACOS;

		if (name.startsWith("solaris") || name.startsWith("sunos"))
			return SOLARIS;

		if (name.startsWith("freebsd"))
			return FREEBSD;

		if (name.startsWith("openbsd"))
			return OPENBSD;

		if (name.startsWith("aix"))
			return AIX;

		return UNRECOGNIZED;
	}

	private PlatformUtil() {
	}
}
