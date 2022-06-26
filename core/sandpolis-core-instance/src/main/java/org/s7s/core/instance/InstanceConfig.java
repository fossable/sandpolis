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
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.s7s.core.instance.InstanceConfig.LoggingConfig;
import org.s7s.core.instance.InstanceConfig.PluginConfig;

public record InstanceConfig(boolean container_resident, boolean development, LoggingConfig logging,
		PluginConfig plugin) {

	private static final Logger log = LoggerFactory.getLogger(InstanceConfig.class);

	public record LoggingConfig(String[] levels) {
	}

	public record PluginConfig(boolean enabled) {
	}

	public static final InstanceConfig EMBEDDED = load();

	private static InstanceConfig load() {
		try (var in = InstanceConfig.class.getResourceAsStream("/org.s7s.core.instance.json")) {
			if (in != null) {
				return new ObjectMapper().readValue(in, InstanceConfig.class);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return null;
	}
}
