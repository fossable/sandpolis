/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.core.instance;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * This singleton contains the instance configuration.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class Config {

	/**
	 * The global configuration.
	 */
	private static final Map<String, Object> config = new HashMap<>();

	/**
	 * Check that the configuration contains a property.
	 * 
	 * @param property The property to request
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
	 * type.
	 * 
	 * @param property The property name
	 * @param type     The property type
	 * @return Whether the environment fufills the requirement
	 */
	public static boolean require(String property, Class<?> type) {
		Objects.requireNonNull(property);
		Objects.requireNonNull(type);

		String value = getValue(property);

		if (value == null)
			return false;

		if (type == String.class)
			config.put(property, value);
		else if (type == Boolean.class)
			config.put(property, Boolean.parseBoolean(value));
		else if (type == Integer.class)
			config.put(property, Integer.parseInt(value));
		else
			throw new IllegalArgumentException("Unsupported type: " + type.getName());

		return true;
	}

	/**
	 * Get a property from the runtime environment or a default value.
	 * 
	 * @param property The property name
	 * @param def      The default value
	 */
	public static void register(String property, Object def) {
		Objects.requireNonNull(property);

		String value = getValue(property);
		if (value == null)
			if (def != null)
				config.put(property, def);
			else if (def instanceof String)
				config.put(property, value);
			else if (def instanceof Boolean)
				config.put(property, Boolean.parseBoolean(value));
			else if (def instanceof Integer)
				config.put(property, Integer.parseInt(value));
			else
				throw new IllegalArgumentException();
	}

	/**
	 * Query a property.
	 * 
	 * @param property The property name
	 * @return The property value
	 */
	private static String getValue(String property) {

		// Query environment variable
		// TODO remove this translation or document
		String value = System.getenv().get(property.toUpperCase().replace('.', '_'));
		if (value != null)
			return value;

		// Query system property
		return System.getProperty(property);
	}

	private Config() {
	}
}
