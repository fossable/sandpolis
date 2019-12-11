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
package com.sandpolis.core.instance;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the instance configuration. It reads values from the
 * following sources in order from highest to lowest precedence:
 *
 * <ul>
 * <li>Main Arguments</li>
 * <li>System Properties</li>
 * <li>Environment Variables</li>
 * </ul>
 *
 * Note: environment variables are uppercase and separated with underscores
 * rather than dots. Therefore the equivalent environment variable for
 * "install.path" is "INSTALL_PATH".
 *
 * @author cilki
 * @since 5.0.0
 */
public final class Config {

	public static final Logger log = LoggerFactory.getLogger(Config.class);

	/**
	 * The global configuration.
	 */
	private static final Map<String, Object> config = new HashMap<>();

	/**
	 * Arguments passed to the program's main.
	 */
	private static String[] args = new String[] {};

	/**
	 * Set the main argument list.
	 *
	 * @param args The main arguments to use for configuration
	 */
	public static void setArguments(String[] args) {
		Config.args = Objects.requireNonNull(args);
	}

	/**
	 * Check that the configuration contains a property.
	 *
	 * @param property The property to check
	 * @return Whether the property exists in the configuration
	 */
	public static boolean has(String property) {
		return config.containsKey(property);
	}

	/**
	 * Get the value of a String property.
	 *
	 * @param property The property to request
	 * @return The property value
	 */
	public static String get(String property) {
		return (String) config.get(property);
	}

	/**
	 * Get the value of a Boolean property.
	 *
	 * @param property The property to request
	 * @return The property value
	 */
	public static boolean getBoolean(String property) {
		return (boolean) config.get(property);
	}

	/**
	 * Get the value of an Integer property.
	 *
	 * @param property The property to request
	 * @return The property value
	 */
	public static int getInteger(String property) {
		return (int) config.get(property);
	}

	/**
	 * Insist that the runtime environment provide the given property of the given
	 * type. If the property is missing, an exception will be thrown.
	 *
	 * @param property The property name
	 * @param type     The property type
	 */
	public static void require(String property, Class<?> type) {
		Objects.requireNonNull(property);
		Objects.requireNonNull(type);
		if (config.containsKey(property))
			log.warn("Overwriting property registration: " + property);

		String value = getValue(property);
		if (value == null)
			throw new RuntimeException("Missing required property: " + property);

		if (type == String.class)
			config.put(property, value);
		else if (type == Boolean.class)
			config.put(property, Boolean.parseBoolean(value));
		else if (type == Integer.class)
			config.put(property, Integer.parseInt(value));
		else
			throw new IllegalArgumentException("Unsupported type: " + type.getName());
	}

	/**
	 * Suggest that the given property may be supplied by the runtime environment.
	 * If the property is missing, the given default will be used. A default of
	 * {@code null} implies the property's type should be {@link String}.
	 *
	 * @param property The property name
	 * @param def      The default value
	 */
	public static void register(String property, Object def) {
		Objects.requireNonNull(property);
		if (config.containsKey(property))
			log.warn("Overwriting property registration: " + property);

		String value = getValue(property);
		if (value == null) {
			// Set default if not null
			if (def != null)
				config.put(property, def);
		} else {
			// Use default to predict type of value
			if (def instanceof Boolean)
				config.put(property, Boolean.parseBoolean(value));
			else if (def instanceof Integer)
				config.put(property, Integer.parseInt(value));
			else
				config.put(property, value);
		}
	}

	/**
	 * Suggest that the given {@link String} property may be supplied by the runtime
	 * environment.
	 *
	 * @param property The property name
	 */
	public static void register(String property) {
		register(property, null);
	}

	/**
	 * Get the store's configuration.
	 *
	 * @return The configuration data
	 */
	public static Set<Entry<String, Object>> entries() {
		return config.entrySet();
	}

	/**
	 * Print the current configuration to the debug logger.
	 */
	public static void print() {
		log.debug("PRINTING CONFIGURATION:\n{}", config.entrySet().stream()
				.map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining("\n")));
	}

	/**
	 * Query the runtime environment for the given property.
	 *
	 * @param property The property name
	 * @return The property value
	 */
	private static String getValue(String property) {
		Objects.requireNonNull(property);

		// Query main argument
		for (String argument : args)
			if (argument.startsWith(property + "="))
				return argument.substring(argument.indexOf('=') + 1);

		// Query system property
		if (System.getProperties().containsKey(property))
			return System.getProperty(property);

		// Query environment variable
		return System.getenv().get(property.toUpperCase().replace('.', '_'));
	}

	private Config() {
	}
}
