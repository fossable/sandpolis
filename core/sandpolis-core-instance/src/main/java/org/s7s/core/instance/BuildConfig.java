//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

public record BuildConfig(String platform, long timestamp, VersionConfig versions,
		List<DependencyConfig> dependencies) {

	public static final BuildConfig EMBEDDED = load();

	public static BuildConfig load() {

		try (var in = Entrypoint.data().main().getResourceAsStream("/org.s7s.build.json")) {
			if (in != null) {
				return new ObjectMapper().readValue(in, BuildConfig.class);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return null;
	}

	public record VersionConfig(

			/**
			 * The instance version.
			 */
			String instance,

			/**
			 * The Gradle version used in the build.
			 */
			String gradle,

			/**
			 * The Java version used in the build.
			 */
			String java) {
	}

	public record DependencyConfig(String group, String artifact, String version, String classifier, String hash) {

	}
}
