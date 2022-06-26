//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server;

import org.s7s.core.foundation.S7SEnvironmentVariable;
import org.s7s.core.foundation.S7SSystemProperty;
import org.s7s.core.instance.RuntimeVariable;

public class ServerContext {

	/**
	 * Whether a server banner will be sent to prospective connections.
	 */
	public static final RuntimeVariable<Boolean> BANNER_ENABLED = RuntimeVariable.of(cfg -> {
		cfg.type = Boolean.class;
		cfg.secondary = S7SSystemProperty.of("s7s.banner");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_BANNER");
		cfg.defaultValue = () -> ServerConfig.EMBEDDED.banner().enabled();
	});

	/**
	 * A path to an image to use in the server banner.
	 */
	public static final RuntimeVariable<String> BANNER_IMAGE = RuntimeVariable.of(cfg -> {
		cfg.type = String.class;
		cfg.secondary = S7SSystemProperty.of("s7s.banner.image");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_BANNER_IMAGE");
		cfg.defaultValue = () -> ServerConfig.EMBEDDED.banner().image();
	});

	/**
	 * A greeting message to use in the server banner.
	 */
	public static final RuntimeVariable<String> BANNER_TEXT = RuntimeVariable.of(cfg -> {
		cfg.type = String.class;
		cfg.secondary = S7SSystemProperty.of("s7s.banner.text");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_BANNER_TEXT");
		cfg.defaultValue = () -> ServerConfig.EMBEDDED.banner().text();
	});

	/**
	 * The service to use for geolocation requests.
	 */
	public static final RuntimeVariable<String> GEOLOCATION_SERVICE = RuntimeVariable.of(cfg -> {
		cfg.type = String.class;
		cfg.secondary = S7SSystemProperty.of("s7s.geolocation.service");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_GEOLOCATION_SERVICE");
		cfg.defaultValue = () -> ServerConfig.EMBEDDED.geolocation().service();
	});

	/**
	 * The geolocation API key.
	 */
	public static final RuntimeVariable<String> GEOLOCATION_SERVICE_KEY = RuntimeVariable.of(cfg -> {
		cfg.type = String.class;
		cfg.secondary = S7SSystemProperty.of("s7s.geolocation.service_key");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_GEOLOCATION_SERVICE_KEY");
		cfg.defaultValue = () -> ServerConfig.EMBEDDED.geolocation().key();
	});

	/**
	 * The geolocation request timeout.
	 */
	public static final RuntimeVariable<Integer> GEOLOCATION_TIMEOUT = RuntimeVariable.of(cfg -> {
		cfg.type = Integer.class;
		cfg.secondary = S7SSystemProperty.of("s7s.geolocation.timeout");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_GEOLOCATION_TIMEOUT");
		cfg.defaultValue = () -> ServerConfig.EMBEDDED.geolocation().timeout();
	});

	/**
	 * The MongoDB endpoint address and port.
	 */
	public static final RuntimeVariable<String> MONGODB_HOST = RuntimeVariable.of(cfg -> {
		cfg.type = String.class;
		cfg.secondary = S7SSystemProperty.of("s7s.storage.mongodb.host");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_MONGODB_HOST");
		cfg.defaultValue = () -> ServerConfig.EMBEDDED.storage().mongodb().host();
	});

	/**
	 * The MongoDB database password.
	 */
	public static final RuntimeVariable<String> MONGODB_PASSWORD = RuntimeVariable.of(cfg -> {
		cfg.type = String.class;
		cfg.secondary = S7SSystemProperty.of("s7s.storage.mongodb.password");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_MONGODB_PASSWORD");
		cfg.defaultValue = () -> ServerConfig.EMBEDDED.storage().mongodb().password();
	});

	/**
	 * The MongoDB database username.
	 */
	public static final RuntimeVariable<String> MONGODB_USER = RuntimeVariable.of(cfg -> {
		cfg.type = String.class;
		cfg.secondary = S7SSystemProperty.of("s7s.storage.mongodb.user");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_MONGODB_USER");
		cfg.defaultValue = () -> ServerConfig.EMBEDDED.storage().mongodb().username();
	});

	/**
	 * The storage provider.
	 */
	public static final RuntimeVariable<String> STORAGE_PROVIDER = RuntimeVariable.of(cfg -> {
		cfg.type = String.class;
		cfg.secondary = S7SSystemProperty.of("s7s.storage.provider");
		cfg.tertiary = S7SEnvironmentVariable.of("S7S_STORAGE_PROVIDER");
		cfg.defaultValue = () -> ServerConfig.EMBEDDED.storage().provider();
	});
}
