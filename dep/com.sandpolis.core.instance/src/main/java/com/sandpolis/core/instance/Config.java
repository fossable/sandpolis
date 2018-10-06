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

import java.io.FileInputStream;
import java.util.Properties;

/**
 * This singleton contains instance configuration constants.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class Config {
	private Config() {
	}

	/**
	 * The database provider.
	 */
	public static final String DB_PROVIDER;

	/**
	 * The database URL.
	 */
	public static final String DB_URL;

	/**
	 * Implications of {@code DEBUG_CLIENT}:
	 * <ul>
	 * <li>A debug client will be installed on the system upon startup</li>
	 * </ul>
	 * 
	 * Default: {@code false}
	 */
	public static final boolean DEBUG_CLIENT;

	/**
	 * Implications of {@code LOG_NET}:
	 * <ul>
	 * <li>Decoded network I/O will be logged</li>
	 * </ul>
	 * 
	 * Default: {@code false}
	 */
	public static final boolean LOG_NET;

	/**
	 * Implications of {@code LOG_NET_RAW}:
	 * <ul>
	 * <li>Undecoded network I/O will be logged</li>
	 * </ul>
	 * 
	 * Default: {@code false}
	 */
	public static final boolean LOG_NET_RAW;

	/**
	 * Implications of {@code NO_MUTEX}:
	 * <ul>
	 * <li>The instance will not check for other running instances of the same
	 * type</li>
	 * </ul>
	 * 
	 * Default: {@code false}
	 */
	public static final boolean NO_MUTEX;

	/**
	 * Implications of {@code NO_SSL}:
	 * <ul>
	 * <li>SSL will be disabled for all connections</li>
	 * </ul>
	 * 
	 * Default: {@code false}
	 */
	public static final boolean NO_SSL;

	/**
	 * Implications of {@code NO_PLUGINS}:
	 * <ul>
	 * <li>Plugins will not be loaded on startup</li>
	 * </ul>
	 * 
	 * Default: {@code false}
	 */
	public static final boolean NO_PLUGINS;

	/**
	 * Implications of {@code NO_TASK_SUMMARY}:
	 * <ul>
	 * <li>The task summary will not be shown on startup</li>
	 * </ul>
	 * 
	 * Default: {@code true}
	 */
	public static final boolean NO_TASK_SUMMARY;

	/**
	 * Implications of {@code POST}:
	 * <ul>
	 * <li>The instance will perform a self-test upon startup</li>
	 * </ul>
	 * 
	 * Default: {@code false}
	 */
	public static final boolean POST;

	static {
		Properties prop = new Properties();
		try (FileInputStream in = new FileInputStream("instance.properties")) {
			prop.load(in);
		} catch (Exception ignore) {
			// The show must go on
		}

		// Merge system properties
		prop.putAll(System.getProperties());

		// Parse properties
		DB_PROVIDER = prop.getProperty("db.provider", "hibernate");
		DB_URL = prop.getProperty("db.url", null);
		DEBUG_CLIENT = Boolean.parseBoolean(prop.getProperty("debug-client", "false"));
		LOG_NET = Boolean.parseBoolean(prop.getProperty("log-net", "false"));
		LOG_NET_RAW = Boolean.parseBoolean(prop.getProperty("log-net-raw", "false"));
		NO_MUTEX = Boolean.parseBoolean(prop.getProperty("no-mutex", "false"));
		NO_SSL = Boolean.parseBoolean(prop.getProperty("no-ssl", "true"));
		NO_PLUGINS = Boolean.parseBoolean(prop.getProperty("no-plugins", "true"));
		NO_TASK_SUMMARY = Boolean.parseBoolean(prop.getProperty("no-summary", "false"));
		POST = Boolean.parseBoolean(prop.getProperty("post", "true"));
	}
}
