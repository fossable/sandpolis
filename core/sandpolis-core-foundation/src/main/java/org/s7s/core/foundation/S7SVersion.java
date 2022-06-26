//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.regex.Pattern;

public record S7SVersion(String version) implements Comparable<S7SVersion> {

	private static class LazyVersionRegex {
		private static final Pattern VALIDATOR = Pattern.compile("^(\\d)+\\.(\\d)+\\.(\\d)+(-(\\d)+)?$");
	}

	@Override
	public int compareTo(S7SVersion o) {
		return Arrays.compare(Arrays.stream(version.split("\\.|\\+")).mapToInt(Integer::parseInt).toArray(),
				Arrays.stream(o.version.split("\\.")).mapToInt(Integer::parseInt).toArray());
	}

	public static S7SVersion of(String version) {
		return new S7SVersion(version);
	}

	public boolean isS7SModuleVersion() {
		return LazyVersionRegex.VALIDATOR.matcher(version).matches();
	}

	/**
	 * Extract the version number out of the JVM --version text.
	 *
	 * @param versionText The version info
	 * @return The version number
	 */
	public static S7SVersion fromJavaVersionText(String versionText) {
		checkNotNull(versionText);

		var matcher = Pattern.compile("\\b([0-9]+\\.[0-9]\\.[0-9])\\b").matcher(versionText);
		if (matcher.find()) {
			return new S7SVersion(matcher.group(1));
		}
		return null;
	}

}
