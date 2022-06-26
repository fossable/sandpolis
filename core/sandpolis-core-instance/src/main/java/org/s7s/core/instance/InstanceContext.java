//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.s7s.core.foundation.S7SEnvironmentVariable;
import org.s7s.core.foundation.S7SSystemProperty;
import org.s7s.core.instance.state.oid.Oid;

public abstract class InstanceContext {

	/**
	 * Whether process mutexes will be checked and enforced.
	 */
	public static final RuntimeVariable<Boolean> MUTEX = RuntimeVariable.of(cfg -> {
		cfg.type = Boolean.class;
		cfg.secondary = S7SSystemProperty.of("s7s.mutex.enabled");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_MUTEX_ENABLED");
		cfg.defaultValue = () -> true;
	});

	/**
	 * The database directory.
	 */
	public static final RuntimeVariable<Path> PATH_DATA = RuntimeVariable.of(cfg -> {
		cfg.type = Path.class;
		cfg.secondary = S7SSystemProperty.of("s7s.path.data");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_PATH_DATA");
		cfg.defaultValue = () -> Entrypoint.data().jar().resolveSibling("data");
	});

	/**
	 * The library directory.
	 */
	public static final RuntimeVariable<Path> PATH_LIB = RuntimeVariable.of(cfg -> {
		cfg.type = Path.class;
		cfg.secondary = S7SSystemProperty.of("s7s.path.lib");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_PATH_LIB");
		cfg.defaultValue = () -> Entrypoint.data().jar().resolveSibling("lib");
	});

	/**
	 * The log output directory.
	 */
	public static final RuntimeVariable<Path> PATH_LOG = RuntimeVariable.of(cfg -> {
		cfg.type = Path.class;
		cfg.secondary = S7SSystemProperty.of("s7s.path.log");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_PATH_LOG");
		cfg.defaultValue = () -> Entrypoint.data().jar().resolveSibling("log");
	});

	/**
	 * The plugin directory.
	 */
	public static final RuntimeVariable<Path> PATH_PLUGIN = RuntimeVariable.of(cfg -> {
		cfg.type = Path.class;
		cfg.secondary = S7SSystemProperty.of("s7s.path.plugin");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_PATH_PLUGIN");
		cfg.defaultValue = () -> Entrypoint.data().jar().resolveSibling("plugin");
	});

	/**
	 * The temporary directory.
	 */
	public static final RuntimeVariable<Path> PATH_TMP = RuntimeVariable.of(cfg -> {
		cfg.type = Path.class;
		cfg.secondary = S7SSystemProperty.of("s7s.path.tmp");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_PATH_TMP");
		cfg.defaultValue = () -> Paths.get(System.getProperty("java.io.tmpdir"));
	});

	public static final RuntimeVariable<String[]> LOG_LEVELS = RuntimeVariable.of(cfg -> {
		cfg.type = String[].class;
		cfg.secondary = S7SSystemProperty.of("s7s.logging.levels");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_LOG_LEVELS");
		cfg.defaultValue = () -> InstanceConfig.EMBEDDED.logging().levels();
	});

	/**
	 * Whether plugins can be loaded.
	 */
	public static final RuntimeVariable<Boolean> PLUGIN_ENABLED = RuntimeVariable.of(cfg -> {
		cfg.type = Boolean.class;
		cfg.secondary = S7SSystemProperty.of("s7s.plugins.enabled");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_PLUGINS_ENABLED");
		cfg.defaultValue = () -> InstanceConfig.EMBEDDED.plugin().enabled();
	});

	/**
	 * Whether development features should be enabled.
	 */
	public static final RuntimeVariable<Boolean> DEVELOPMENT_MODE = RuntimeVariable.of(cfg -> {
		cfg.type = Boolean.class;
		cfg.secondary = S7SSystemProperty.of("s7s.development");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_DEVELOPMENT");
		cfg.defaultValue = () -> InstanceConfig.EMBEDDED.development();
	});

	/**
	 * The default message timeout in milliseconds.
	 */
	public static final RuntimeVariable<Integer> MESSAGE_TIMEOUT = RuntimeVariable.of(cfg -> {
		cfg.type = Integer.class;
		cfg.primary = Oid.of("org.s7s.core.instance:/profile/*/message_timeout");
		cfg.secondary = S7SSystemProperty.of("s7s.net.message_timeout");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_MESSAGE_TIMEOUT");
		cfg.defaultValue = () -> 1000;
	});

	/**
	 * The maximum number of outgoing connection attempts.
	 */
	public static final RuntimeVariable<Integer> OUTGOING_CONCURRENCY = RuntimeVariable.of(cfg -> {
		cfg.type = Integer.class;
		cfg.secondary = S7SSystemProperty.of("s7s.net.connection.max_outgoing");
		cfg.defaultValue = () -> 4;
		cfg.validator = value -> {
			return value > 0;
		};
	});

	/**
	 * Whether TLS will be used for network connections.
	 */
	public static final RuntimeVariable<Boolean> TLS_ENABLED = RuntimeVariable.of(cfg -> {
		cfg.type = Boolean.class;
		cfg.secondary = S7SSystemProperty.of("s7s.net.connection.tls");
	});

	/**
	 * Whether decoded network traffic will be logged.
	 */
	public static final RuntimeVariable<Boolean> LOG_TRAFFIC_DECODED = RuntimeVariable.of(cfg -> {
		cfg.type = Boolean.class;
		cfg.secondary = S7SSystemProperty.of("s7s.net.logging.decoded");
	});

	/**
	 * The traffic statistics interval.
	 */
	public static final RuntimeVariable<Integer> TRAFFIC_INTERVAL = RuntimeVariable.of(cfg -> {
		cfg.type = Integer.class;
		cfg.secondary = S7SSystemProperty.of("s7s.net.connection.stat_interval");
	});

	/**
	 * Whether raw network traffic will be logged.
	 */
	public static final RuntimeVariable<Boolean> LOG_TRAFFIC_RAW = RuntimeVariable.of(cfg -> {
		cfg.type = Boolean.class;
		cfg.secondary = S7SSystemProperty.of("s7s.net.logging.raw");
	});

}
