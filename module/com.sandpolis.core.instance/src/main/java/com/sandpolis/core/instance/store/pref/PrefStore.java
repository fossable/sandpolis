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
package com.sandpolis.core.instance.store.pref;

import java.util.Arrays;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Store;

/**
 * This store provides access to a unique {@link Preferences} object for
 * persistent instance settings.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class PrefStore extends Store {

	private static final Logger log = LoggerFactory.getLogger(PrefStore.class);

	/**
	 * The backing {@link Preferences} object.
	 */
	private static Preferences provider;

	/**
	 * Initialize the store from the given {@link Preferences}.
	 * 
	 * @param prefs The store provider
	 */
	public static void init(Preferences prefs) {
		if (provider != null)
			throw new IllegalStateException("Attempted to reinitialize an unclosed store");

		provider = Objects.requireNonNull(prefs);
	}

	public static void load(Class<?> c) {
		init(Preferences.userNodeForPackage(Objects.requireNonNull(c)));
	}

	/**
	 * Get a value from the store.
	 * 
	 * @param tag A unique String whose associated value is to be returned
	 * @return The String value associated with the provided tag
	 */
	public static String getString(String tag) {
		return getString(tag, null);
	}

	/**
	 * Get a value from the store.
	 * 
	 * @param tag A unique String whose associated value is to be returned
	 * @param def The default value
	 * @return The String value associated with the provided tag
	 */
	public static String getString(String tag, String def) {
		return provider.get(tag, def);
	}

	/**
	 * Add a value to the store. Old values are overwritten.
	 * 
	 * @param tag   The unique key which will become associated with the new value
	 * @param value The new value
	 */
	public static void putString(String tag, String value) {
		log.trace("Associating \"{}\": \"{}\"", tag, value);
		provider.put(tag, value);
	}

	/**
	 * Get a value from the store.
	 * 
	 * @param tag A unique String whose associated value is to be returned
	 * @return The boolean value associated with the provided tag
	 */
	public static boolean getBoolean(String tag) {
		return provider.getBoolean(tag, false);
	}

	/**
	 * Add a value to the store. Old values are overwritten.
	 * 
	 * @param tag   The unique key which will become associated with the new value
	 * @param value The new value
	 */
	public static void putBoolean(String tag, boolean value) {
		log.trace("Associating \"{}\": {}", tag, value);
		provider.putBoolean(tag, value);
	}

	/**
	 * Get a value from the store.
	 * 
	 * @param tag A unique String whose associated value is to be returned
	 * @return The int value associated with the provided tag
	 */
	public static int getInt(String tag) {
		return provider.getInt(tag, 0);
	}

	/**
	 * Add a value to the store. Old values are overwritten.
	 * 
	 * @param tag   The unique key which will become associated with the new value
	 * @param value The new value
	 */
	public static void putInt(String tag, int value) {
		log.trace("Associating \"{}\": {}", tag, value);
		provider.putInt(tag, value);
	}

	/**
	 * Get a value from the store.
	 * 
	 * @param tag A unique String whose associated value is to be returned
	 * @return The byte[] value associated with the provided tag
	 */
	public static byte[] getBytes(String tag) {
		return provider.getByteArray(tag, null);
	}

	/**
	 * Add a value to the store. Old values are overwritten.
	 * 
	 * @param tag   The unique key which will become associated with the new value
	 * @param value The new value
	 */
	public static void putBytes(String tag, byte[] value) {
		log.trace("Associating \"{}\": {}", tag, value);
		provider.putByteArray(tag, value);
	}

	/**
	 * Add a general value to the store. Old values are overwritten.
	 * 
	 * @param tag   The unique key which will become associated with the new value
	 * @param value The new value
	 */
	public static void put(String tag, Object value) {
		Objects.requireNonNull(tag);
		Objects.requireNonNull(value);

		if (value instanceof String)
			putString(tag, (String) value);
		else if (value instanceof Boolean)
			putBoolean(tag, (Boolean) value);
		else if (value instanceof Integer)
			putInt(tag, (Integer) value);
		else if (value instanceof byte[])
			putBytes(tag, (byte[]) value);
		else
			throw new IllegalArgumentException("Cannot store value of type: " + value.getClass().getName());
	}

	/**
	 * Add a value to the store unless it's already present.
	 * 
	 * @param tag   The unique key which will become associated with the new value
	 *              unless it's already associated with a value
	 * @param value The new value
	 * @throws BackingStoreException
	 */
	public static void register(String tag, Object value) throws BackingStoreException {
		Objects.requireNonNull(tag);
		Objects.requireNonNull(value);

		if (Arrays.stream(provider.keys()).noneMatch(key -> tag.equals(key))) {
			put(tag, value);
		}
	}

	/**
	 * Flush and shutdown the store. Any subsequent interaction will fail until the
	 * store is reinitialized.
	 */
	public static void close() throws BackingStoreException {
		try {
			provider.flush();
		} finally {
			provider = null;
		}
	}

	private PrefStore() {
	}
}
