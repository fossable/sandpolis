//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.foundation;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the instance configuration. It reads values from the
 * following sources in order from highest to lowest precedence:
 *
 * <ul>
 * <li>System Properties</li>
 * <li>Environment Variables</li>
 * </ul>
 *
 * Note: environment variables are uppercase and separated with underscores
 * rather than dots. Therefore the equivalent environment variable for
 * "install.path" is "INSTALL_PATH".
 *
 * @since 5.0.0
 */
public final class Config {

	private static final Logger log = LoggerFactory.getLogger(Config.class);

	public static final class ConfigProperty<T> {

		/**
		 * Whether the property has been "evaluated".
		 */
		private boolean evaluated;

		/**
		 * The property's name.
		 */
		private final String property;

		/**
		 * The property's data type.
		 */
		private final Class<T> type;

		/**
		 * The property's data value.
		 */
		private T value;

		private ConfigProperty(Class<T> type, String property) {
			this.type = Objects.requireNonNull(type);
			this.property = Objects.requireNonNull(property);
		}

		/**
		 * Get whether the property was defined by the runtime environment.
		 *
		 * @return Whether the property has a non-null value
		 */
		public boolean defined() {
			evaluate();
			return value != null;
		}

		/**
		 * Attempt to pull a value for the property from the runtime environment. System
		 * properties have highest priority and then environment variables come next.
		 */
		@SuppressWarnings("unchecked")
		private void evaluate() {
			if (evaluated)
				return;
			evaluated = true;

			// First priority: system properties
			String value = System.getProperty(property);
			if (value == null) {
				// Second priority: environment variables
				value = System.getenv().get(property.toUpperCase().replace('.', '_'));
				if (value == null) {
					log.trace("Failed to load property: {}", property);
					return;
				} else {
					log.trace("Loaded environment variable: {} = {}", property.toUpperCase().replace('.', '_'), value);
				}
			} else {
				log.trace("Loaded system property: {} = {}", property, value);
			}

			try {
				if (type == String.class) {
					this.value = (T) (String) value;
				} else if (type == Integer.class) {
					this.value = (T) (Integer) Integer.parseInt(value);
				} else if (type == Boolean.class) {
					this.value = (T) (Boolean) Boolean.parseBoolean(value);
				}
			} catch (Exception e) {
				log.error("Failed to parse property: {}", property);
			}
		}

		/**
		 * Get the name of this property.
		 *
		 * @return The property name
		 */
		public String property() {
			return property;
		}

		/**
		 * Suggest that the runtime environment provide a value for this property. If no
		 * value is found, the property will remain be "undefined".
		 */
		public void register() {
			evaluate();
		}

		/**
		 * Suggest that the runtime environment provide a value for this property. If no
		 * value is found, the given default will be used.
		 *
		 * @param _default A default property value
		 */
		public void register(T _default) {
			evaluate();
			if (value == null)
				value = _default;
		}

		/**
		 * Insist that the runtime environment provide a value for this property. If no
		 * value is found, an exception will be thrown.
		 */
		public void require() {
			evaluate();
			if (!defined())
				throw new RuntimeException("Required property not defined: " + property);
		}

		/**
		 * Get the value of this property.
		 *
		 * @return The property value
		 */
		public Optional<T> value() {
			evaluate();
			return Optional.ofNullable(value);
		}
	}

	/**
	 * Whether a server banner will be sent to prospective connections.
	 * <p>
	 * <b> Compatible instances: server</b>
	 */
	public static final ConfigProperty<Boolean> BANNER_ENABLED = new ConfigProperty<>(Boolean.class,
			"sandpolis.banner");

	/**
	 * A path to an image to use in the server banner.
	 * <p>
	 * <b> Compatible instances: server</b>
	 */
	public static final ConfigProperty<String> BANNER_IMAGE = new ConfigProperty<>(String.class,
			"sandpolis.banner.image");

	/**
	 * A greeting message to use in the server banner.
	 * <p>
	 * <b> Compatible instances: server</b>
	 */
	public static final ConfigProperty<String> BANNER_TEXT = new ConfigProperty<>(String.class,
			"sandpolis.banner.text");

	/**
	 * Whether the instance is in "configuration mode".
	 * <p>
	 * <b> Compatible instances: agent</b>
	 */
	public static final ConfigProperty<Boolean> CONFIG_MODE = new ConfigProperty<>(Boolean.class,
			"sandpolis.config_mode");

	/**
	 * The database user password.
	 * <p>
	 * <b> Compatible instances: server</b>
	 */
	public static final ConfigProperty<String> DB_PASSWORD = new ConfigProperty<>(String.class,
			"sandpolis.database.password");

	/**
	 * The database provider name.
	 * <p>
	 * <b> Compatible instances: server</b>
	 */
	public static final ConfigProperty<String> DB_PROVIDER = new ConfigProperty<>(String.class,
			"sandpolis.database.provider");

	/**
	 * The database URL.
	 * <p>
	 * <b> Compatible instances: server</b>
	 */
	public static final ConfigProperty<String> DB_URL = new ConfigProperty<>(String.class, "sandpolis.database.url");

	/**
	 * The database user username.
	 * <p>
	 * <b> Compatible instances: server</b>
	 */
	public static final ConfigProperty<String> DB_USERNAME = new ConfigProperty<>(String.class,
			"sandpolis.database.username");

	/**
	 * Whether a debug agent will be generated on startup.
	 * <p>
	 * <b> Compatible instances: server</b>
	 */
	public static final ConfigProperty<Boolean> DEBUG_AGENT = new ConfigProperty<>(Boolean.class,
			"sandpolis.debug.generate_agent");

	/**
	 * The service to use for geolocation requests.
	 * <p>
	 * <b> Compatible instances: server</b>
	 */
	public static final ConfigProperty<String> GEOLOCATION_SERVICE = new ConfigProperty<>(String.class,
			"sandpolis.geolocation.service");

	/**
	 * The geolocation API key.
	 * <p>
	 * <b> Compatible instances: server</b>
	 */
	public static final ConfigProperty<String> GEOLOCATION_SERVICE_KEY = new ConfigProperty<>(String.class,
			"sandpolis.geolocation.service_key");

	/**
	 * The geolocation request timeout.
	 * <p>
	 * <b> Compatible instances: server</b>
	 */
	public static final ConfigProperty<Integer> GEOLOCATION_TIMEOUT = new ConfigProperty<>(Integer.class,
			"sandpolis.geolocation.timeout");

	/**
	 * Whether process mutexes will be checked and enforced.
	 */
	public static final ConfigProperty<Boolean> MUTEX = new ConfigProperty<>(Boolean.class, "sandpolis.mutex");

	/**
	 * The default message timeout in milliseconds.
	 */
	public static final ConfigProperty<Integer> MESSAGE_TIMEOUT = new ConfigProperty<>(Integer.class,
			"sandpolis.net.message_timeout");

	/**
	 * The maximum number of outgoing connection attempts.
	 */
	public static final ConfigProperty<Integer> OUTGOING_CONCURRENCY = new ConfigProperty<>(Integer.class,
			"sandpolis.net.connection.max_outgoing");

	/**
	 * The database directory.
	 * <p>
	 * <b> Compatible instances: server</b>
	 */
	public static final ConfigProperty<String> PATH_DATA = new ConfigProperty<>(String.class, "sandpolis.path.db");

	/**
	 * The configuration directory.
	 */
	public static final ConfigProperty<String> PATH_CFG = new ConfigProperty<>(String.class, "sandpolis.path.config");

	/**
	 * The library directory.
	 */
	public static final ConfigProperty<String> PATH_LIB = new ConfigProperty<>(String.class, "sandpolis.path.lib");

	/**
	 * The log output directory.
	 */
	public static final ConfigProperty<String> PATH_LOG = new ConfigProperty<>(String.class, "sandpolis.path.log");

	/**
	 * The plugin directory.
	 */
	public static final ConfigProperty<String> PATH_PLUGIN = new ConfigProperty<>(String.class,
			"sandpolis.path.plugin");

	/**
	 * The temporary directory.
	 */
	public static final ConfigProperty<String> PATH_TMP = new ConfigProperty<>(String.class, "sandpolis.path.tmp");

	/**
	 * Whether plugins will be loaded.
	 */
	public static final ConfigProperty<Boolean> PLUGIN_ENABLED = new ConfigProperty<>(Boolean.class,
			"sandpolis.plugins.enabled");

	/**
	 * Whether the startup summary will be logged.
	 */
	public static final ConfigProperty<Boolean> STARTUP_SUMMARY = new ConfigProperty<>(Boolean.class,
			"sandpolis.startup.logging.summary");

	/**
	 * Whether TLS will be used for network connections.
	 */
	public static final ConfigProperty<Boolean> TLS_ENABLED = new ConfigProperty<>(Boolean.class,
			"sandpolis.net.connection.tls");

	/**
	 * Whether decoded network traffic will be logged.
	 */
	public static final ConfigProperty<Boolean> TRAFFIC_DECODED = new ConfigProperty<>(Boolean.class,
			"sandpolis.net.logging.decoded");

	/**
	 * The traffic statistics interval.
	 */
	public static final ConfigProperty<Integer> TRAFFIC_INTERVAL = new ConfigProperty<>(Integer.class,
			"sandpolis.net.connection.stat_interval");

	/**
	 * Whether raw network traffic will be logged.
	 */
	public static final ConfigProperty<Boolean> TRAFFIC_RAW = new ConfigProperty<>(Boolean.class,
			"sandpolis.net.logging.raw");

	/**
	 * The storage provider which may be: mongodb, infinispan_embedded, or
	 * ephemeral.
	 */
	public static final ConfigProperty<String> STORAGE_PROVIDER = new ConfigProperty<>(String.class,
			"sandpolis.storage.provider");

	/**
	 * The MongoDB endpoint address and port.
	 */
	public static final ConfigProperty<String> MONGODB_HOST = new ConfigProperty<>(String.class,
			"sandpolis.storage.mongodb.host");

	/**
	 * The MongoDB database name.
	 */
	public static final ConfigProperty<String> MONGODB_DATABASE = new ConfigProperty<>(String.class,
			"sandpolis.storage.mongodb.database");

	/**
	 * The MongoDB database username.
	 */
	public static final ConfigProperty<String> MONGODB_USER = new ConfigProperty<>(String.class,
			"sandpolis.storage.mongodb.user");

	/**
	 * The MongoDB database password.
	 */
	public static final ConfigProperty<String> MONGODB_PASSWORD = new ConfigProperty<>(String.class,
			"sandpolis.storage.mongodb.password");

	private Config() {
	}
}
